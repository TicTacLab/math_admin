(function() {

var downloadAnchor = $("#download-anchor");
var exportInParamsBtn = $("#export-in-params-btn");
var exportOutParamsBtn = $("#export-out-params-btn");
var importInParamsBtn = $("#import-in-params-btn");
// var inputParams = $(".model-input-params input[type='text']");
var inParamsContainer = $(".model-input-params");
var inputParams = inParamsContainer.find("input[type='text']");
var inParamsFile = $("#in-params-file");

var getInParams = function() {
    var res = {};
    inputParams.each(function(i, domEl) {
      var jqEl = $(domEl);
      var value = jqEl.val();
      var name = jqEl.attr('name');
      res[name] = value;
    });
    return res;
};

var getOutParams = function() {
    return window.OUT_PARAMS;
};

var downloadJson = function(data, fileName) {
  var dataUrl = "data:text/json;charset=utf-8," + encodeURIComponent(JSON.stringify(data, null, 2));

  downloadAnchor.attr("href", dataUrl);
  downloadAnchor.attr("download", fileName);
  downloadAnchor[0].click();
};

var fillInParams = function(inParams) {
  for(name in inParams) {
    var el = inParamsContainer.find("input[type='text'][name='" + name + "']");
    var value = inParams[name];
    el.val(value);
  }
};

exportInParamsBtn.click(function() {
  downloadJson(getInParams(), "in-params.json");
});

exportOutParamsBtn.click(function() {
  downloadJson(getOutParams(), "out-params.json");
});

importInParamsBtn.click(function() {
  inParamsFile[0].click();
});


inParamsFile.change(function(event) {
  var file = event.target.files[0];
  var reader = new FileReader();
  reader.onload = function(e) {
    var inParams = JSON.parse(e.target.result);
    fillInParams(inParams);
    alert("DONE");
  };
  reader.readAsText(file);
});

if(!window.OUT_PARAMS) exportOutParamsBtn.remove();

$('a#back').click(function() {
  var parts = window.location.pathname.split( '/' );
  var modelId = parts[2];
  var revId = parts[3];
  $.ajax({url: '/models/' + modelId + '/' + revId + '/session',
          method: 'delete',
          timeout: 300,
          async: false});
  return true;

});

})();

