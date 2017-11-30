var xmlHttp = new XMLHttpRequest();
xmlHttp.open('GET', 'http://example.org/xhrg', true);
xmlHttp.send(null);

var xmlHttp2 = new XMLHttpRequest();
xmlHttp2.open("POST", "http://example.org/xhrp", true);
xmlHttp2.send(null);