layout "layouts/main.gsp",
title: "LAPPS/EAGER",
version: version,
include: 'js/form.js',
javascript: '''
$(document).ready(function () {
    disable('#submit');
});


''',
content: {
    if (username) {
        div {
            h1 'You previously entered'
            table {
                tr {
                    td 'User name'
                    td username
                }
                tr {
                    td 'Dataset name'
                    td dataset
                }
            }
        }
    }

    h1 "Testing"
    h3 "Rate These Answers"
    div id:'rating-buttons', class:'box', {
        input id:'rate-bad', type:'button', class:'btn-error', value:'Bad', onclick:'rate("123", -1)', ''
        input id:'rate-meh', type:'button', class:'btn-warn', value:'Meh', onclick:'rate("123", 0)', ''
        input id:'rate-good', type:'button', class:'btn-ok', value:'Good', onclick:'rate("123", 1)', ''
    }
    div id:'rating-display', class:'box hidden', {
        p id:'rating', 'Not yet rated.'
    }
    form(action:'test', method:'post') {
        label(for:'username', 'User name:')
        input(id:'username', name:'username', type:'text', size:40, oninput:'validate(this)', '')
        label(for:'dataset', 'Dataset name: ')
        input(id:'dataset', name:'dataset', type:'text', size:40, '')
        div(class:'alert hidden', id:'msgbox', '')
        input(type:'submit', id:'submit', class:'button', value:'Save')
    }
}