let app = angular.module('app', []);
let API = '../backend/api.php';


app.controller('BrowseCtrl', ($scope, $http) => {
  $scope.types = [];
  $scope.recipes = [];
  $scope.selectedType = '';
  $scope.prevFilter = 'none';

  $scope.loadRecipes = () => {
    let type = $scope.selectedType;
    $scope.prevFilter = type === '' ? 'All' : type;
    $http.get(API + '?action=list&type=' + type).then((response) => {
      $scope.recipes = response.data;
      if (type === '') {
        let seen = {};
        $scope.types = [];
        for (const recipe of $scope.recipes) {
          if (!seen[recipe.type]) {
            seen[recipe.type] = 1;
            $scope.types.push(recipe.type);
          }
        }
        $scope.types.sort();
      }
    });
  };

  $scope.loadRecipes();
});

app.controller('DeleteCtrl', ($scope, $http) => {
  $scope.items = [];
  $scope.error = '';

  $scope.load = () => {
    $http.get(API + '?action=list').then((response) => {
      $scope.items = response.data;
    });
  };
  $scope.remove = (id) => {
    $http.post(API + '?action=delete', { id: id }).then($scope.load, () => {
      $scope.error = 'Delete failed.';
    });
  };

  $scope.load();
});

app.controller('AddCtrl', ($scope, $http, $window) => {
  $scope.form = { author: '', name: '', type: '', recipe: '' };
  $scope.error = '';

  $scope.save = () => {
    $scope.error = '';
    $http.post(API + '?action=add', $scope.form).then(() => {
      $window.location.href = 'manage.html';
    }, () => {
      $scope.error = 'Insert failed.';
    });
  };
});

app.controller('EditCtrl', ($scope, $http, $window) => {
  $scope.error = '';

  let id = parseInt(new URLSearchParams(window.location.search).get('id') || '0', 10);

  $http.get(API + '?action=list&id=' + id).then((r) => {
    if (r.data && r.data[0]) {
      $scope.form = r.data[0];
    } else {
      $scope.error = 'Recipe not found.';
    }
  }, () => {
    $scope.error = 'Could not load recipe.';
  });

  $scope.save = () => {
    $scope.error = '';
    $http.post(API + '?action=update', $scope.form).then(() => {
      $window.location.href = 'manage.html';
    }, () => {
      $scope.error = 'Update failed.';
    });
  };
});
