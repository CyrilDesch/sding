const types = require('./conventional-types.json');

module.exports = {
    extends: ['@commitlint/config-conventional'],
    rules: {
        'type-enum': [2, 'always', types]
    }
};
