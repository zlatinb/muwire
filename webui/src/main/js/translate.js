// map is standard json mapping, generated in the .jsp
// and then passed to initTranslate()
// map = '{ "Host" : "xxx", "Search" : "yyy" }'

var translations = new Map();

function initTranslate(map) {
    var obj = JSON.parse(map);
    Object.keys(obj).forEach(k => { translations.set(k, obj[k]) });
}

function _t(s) {
    var rv = translations.get(s);
    if (rv == null) {
        rv = s;
    }
    return rv;
}

// s must contain {0}
// p will replace {0}
function _t(s, p) {
    var rv = translations.get(s);
    if (rv == null) {
        rv = s;
    }
    rv = rv.replace("{0}", p);
    return rv;
}
