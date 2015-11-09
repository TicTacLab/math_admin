(function() {
var parts = window.location.pathname.split('/');
var eventId = parts[4];
var destroyUrl = '/sengine/files/profile/' + eventId + '/destroy';
$('#destroy-session').attr('action', destroyUrl);
var downloadWorkbookUrl = '/sengine/files/profile/' + eventId + '/workbook';
$('#download-workbook').attr('action', downloadWorkbookUrl);

})();
