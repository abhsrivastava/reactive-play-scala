function appendTweet(text) {
    var tweet = document.createElement("p");
    var message = document.createTextNode(text);
    tweet.appendChild(message);
    document.getElementById("tweets").appendChild(tweet);
}

function connect(attempt, url) {
    console.log("going to connect " + url);
    var connectionAttempt = attempt;
    var tweetSocket = new WebSocket(url);
    tweetSocket.onmessage = function(event) {
        console.log("came inside on message");
        console.log(event);
        var data = JSON.parse(event.data);
        appendTweet(data.text);
        console.log("completed on message");
    };
    tweetSocket.onopen = function(event) {
        console.log("came inside on open");
        connectionAttempt = 1;
        tweetSocket.send("subscribe");
        console.log("completed on open");
    };
    tweetSocket.onclose = function(event) {
        console.log("came inside on close");
        if (connectionAttempt <= 3) {
            appendTweet("connection to server has been lost. attempt number  " + connectionAttempt);
            setTimeout(function() {
                connect(connectionAttempt + 1, url);
            }, 5000);
        } else {
            appendTweet("connection to server lost. max retries over");
        }
        console.log("completd on close");
    };
    console.log("completed connect method");
}
