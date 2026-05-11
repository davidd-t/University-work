<?php
require_once 'db.php';

$input = json_decode(file_get_contents('php://input'), true) ?? [];

function fail($msg, $code = 400) {
    http_response_code($code);
    echo json_encode(['error' => $msg]);
    exit;
}

function validate_recipe($author, $name, $type, $recipe) {
    if ($author === '' || $name === '' || $type === '' || $recipe === '') {
        return 'All fields are required.';
    }
    if (strlen($author) < 2) {
        return 'Author must be at least 2 characters.';
    }
    if (strlen($name) < 3) {
        return 'Name must be at least 3 characters.';
    }
    if (strlen($type) < 3) {
        return 'Type must be at least 3 characters.';
    }
    if (strlen($recipe) < 10) {
        return 'Recipe must be at least 10 characters.';
    }
    return '';
}

$action = $_REQUEST['action'] ?? '';

if ($action === 'list') {
    $id = (int)($_GET['id'] ?? 0);
    $type = trim($_GET['type'] ?? '');

    $stmt = $conn->prepare(
    "SELECT id, author, name, type, recipe 
    FROM recipes 
    WHERE (:id = 0 OR id = :id) AND (:type = '' OR type = :type) 
    ORDER BY id DESC");

    $stmt->bindValue(':id', $id, SQLITE3_INTEGER);
    $stmt->bindValue(':type', $type, SQLITE3_TEXT);
    $res = $stmt->execute();

    $items = [];
    if ($res) {
        while ($row = $res->fetchArray(SQLITE3_ASSOC)) {
            $items[] = [
                'id' => (int)$row['id'],
                'author' => $row['author'],
                'name' => $row['name'],
                'type' => $row['type'],
                'recipe' => $row['recipe']
            ];
        }
    }
    echo json_encode($items);
    exit;
}

if ($action === 'add') {
    $author = trim($input['author'] ?? '');
    $name = trim($input['name'] ?? '');
    $type = trim($input['type'] ?? '');
    $recipe = trim($input['recipe'] ?? '');

    $err = validate_recipe($author, $name, $type, $recipe);
    if ($err !== '') {
        fail($err);
    }

    $stmt = $conn->prepare("INSERT INTO recipes(author, name, type, recipe) VALUES(:author, :name, :type, :recipe)");
    $stmt->bindValue(':author', $author, SQLITE3_TEXT);
    $stmt->bindValue(':name', $name, SQLITE3_TEXT);
    $stmt->bindValue(':type', $type, SQLITE3_TEXT);
    $stmt->bindValue(':recipe', $recipe, SQLITE3_TEXT);

    if ($stmt->execute() === false) {
        fail('Insert failed.', 500);
    }

    echo json_encode([]);
    exit;
}

if ($action === 'update') {
    $id = (int)($input['id'] ?? 0);
    $author = trim($input['author'] ?? '');
    $name = trim($input['name'] ?? '');
    $type = trim($input['type'] ?? '');
    $recipe = trim($input['recipe'] ?? '');

    if ($id <= 0) {
        fail('Invalid id.');
    }

    $err = validate_recipe($author, $name, $type, $recipe);
    if ($err !== '') {
        fail($err);
    }

    $stmt = $conn->prepare("UPDATE recipes SET author = :author, name = :name, type = :type, recipe = :recipe WHERE id = :id");
    $stmt->bindValue(':author', $author, SQLITE3_TEXT);
    $stmt->bindValue(':name', $name, SQLITE3_TEXT);
    $stmt->bindValue(':type', $type, SQLITE3_TEXT);
    $stmt->bindValue(':recipe', $recipe, SQLITE3_TEXT);
    $stmt->bindValue(':id', $id, SQLITE3_INTEGER);

    if ($stmt->execute() === false) {
        fail('Update failed.', 500);
    }

    echo json_encode([]);
    exit;
}

if ($action === 'delete') {
    $id = (int)($input['id'] ?? 0);
    if ($id <= 0) {
        fail('Invalid id.');
    }

    $stmt = $conn->prepare("DELETE FROM recipes WHERE id = :id");
    $stmt->bindValue(':id', $id, SQLITE3_INTEGER);
    if ($stmt->execute() === false) {
        fail('Delete failed.', 500);
    }

    echo json_encode([]);
    exit;
}

fail('Invalid action.');
