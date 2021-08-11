module.exports = {
    configure: function (config, successCallback, errorCallback) {
        cordova.exec(successCallback, errorCallback, "WeFitterSamsung", "configure", [config]);
    },
    connect: function (successCallback, errorCallback) {
        cordova.exec(successCallback, errorCallback, "WeFitterSamsung", "connect", []);
    },
    disconnect: function (successCallback, errorCallback) {
        cordova.exec(successCallback, errorCallback, "WeFitterSamsung", "disconnect", []);
    },
    isConnected: function (successCallback, errorCallback) {
        cordova.exec(successCallback, errorCallback, "WeFitterSamsung", "isConnected", []);
    },
    tryToResolveError: function (error, successCallback, errorCallback) {
        cordova.exec(successCallback, errorCallback, "WeFitterSamsung", "tryToResolveError", [error]);
    }
}