var globalSpace = {};
$(function(){
    disableStop();
    $("#searchButton").click(function(){
        filterTweets();
    });
    $("#stopButton").click(function() {
        stopFilter();
        disableStop();
    });
});

function filterTweets() {
    var tweetFilters = $("#filters").val();
    console.log('tweetFilters', tweetFilters);
    if(tweetFilters != undefined && tweetFilters != '') {
        disableSearch()
        $.post("/api/v1/stream/start", function(data) {
            var jsonData = JSON.parse(data);
            var streamId = jsonData["id"];
            var eventBus = new EventBus("http://localhost:9999/eventbus");
            globalSpace["eventBus"] = eventBus;
            globalSpace["streamId"] = streamId;
            eventBus.onopen = function() {
                eventBus.registerHandler("messageToClient_" + streamId, function(error, message) {
                    var currentValue = $("#tweets").val();
                    currentValue = JSON.parse(message.body)["tweet"] + "\n" + currentValue;
                    $("#tweets").val(currentValue);
                });
                eventBus.send("messageToServer", JSON.stringify({command: "start", clientId: streamId, filters:
                    tweetFilters}));
            }
        });
    } else {
        disableStop();
        alert("You need to give comma separated filters.")
    }
}

function stopFilter() {
    var eventBus = globalSpace["eventBus"];
    var streamId = globalSpace["streamId"];
    $.post("/api/v1/stream/" + streamId + "/stop", function(data){
        console.log("stopped stream " + streamId, data);
    });
    eventBus.unregisterHandler("messageToClient_" + streamId , function(error, message) {
       console.log(error, message);
       eventBus.close();
    });
    reset();
}
function disableStop() {
	$("#stopButton").hide();
	$("#searchButton").show();
}

function disableSearch() {
    $("#stopButton").show();
    $("#searchButton").hide();
}

function reset() {
	$("#stopButton").hide();
	$("#searchButton").show();
	$("#tweets").val('');
	$("#filters").val('');
}

function search(socket) {
    var filters = $("#filters").val();
    socket.emit('search', filters);
}

function stop() {

}
