layout 'layouts/main.gsp',
title: 'LAPPS/EAGER',
version: version,
include: 'js/form.js',
javascript: '''
$(window).load(function() {
    $(".loader").fadeOut("slow");
});
$(document).ready(function(){
    //disable("#submit");
    $("#toggle-button").click(function() {
        $(".scores").toggle()
        var btn = $("#toggle-button")
        if (btn.text() === 'Show all document scores') {
            btn.html('Hide document scores')
        }
        else {
            btn.html('Show all document scores')
        }
    })
});
''',
css: '''
#toggle-button {
    margin-bottom: 0.5rem;
}
''',
content: {
    div(class:'loader', '')
    h2 "Rate These Answers"
    div id:'rating-buttons', class:'box', {
        input id:'rate-good', type:'button', class:'btn-ok', value:'Good', onclick:"rate('$key', 1)", ''
        input id:'rate-meh', type:'button', class:'btn-warn', value:'Meh', onclick:"rate('$key', 0)", ''
        input id:'rate-bad', type:'button', class:'btn-error', value:'Bad', onclick:"rate('$key', -1)", ''
    }
    div id:'rating-display', class:'box hidden', {
        p id:'rating', 'Not yet rated.'
    }

    div(class:'section') {
        h1 "You Asked"
        p data.query.question
        p {
            a href:'ask', class:'plain', 'Ask another question'
        }
    }

    div(class:'section') {
        h1 'The Answers'
        button(id:'toggle-button', "Show all document scores")
        table(class:'answers grid scores hidden') {
            tr {
                th 'Index'
                th 'Score'
                th 'ID'
                th 'Year'
                th 'Title'
                if (data.keys) {
                    data.keys.each { key ->
                        data.documents[0].scores[key].each { e ->
                            th(e.key)
                        }
                        th(key)
                    }
                }

            }
            data.documents.eachWithIndex { doc, i ->
                tr {
                    td String.format("%4d", i)
                    td String.format("%2.3f", doc.score)
                    if (doc.pmc) {
                        td { a(href:doc.url, target:'_blank', doc.pmc) }
                    }
                    else if (doc.doi) {
                        td { a(href:doc.url, target:'_blank', doc.doi.substring(0,9) + "...") }
                    }
                    else {
                        td { a(href:doc.url, target:'_blank', doc.id.substring(0,9) + "...") }
                    }
                    td doc.year
                    td doc.title.text
                    if (data.keys) {
                        data.keys.each { key ->
                            doc.scores[key].each { e ->
                                td String.format("%2.3f", e.value)
                                //td e.value
                            }
                            td class:'sum', String.format("%2.3f", doc.scores[key].sum())
                        }
                    }
                }
            }
        }
        table(class:'answers grid scores') {
            tr {
                th 'Index'
                th 'Score'
                th 'ID'
                th 'Year'
                th 'Title'
            }
            data.documents.eachWithIndex { doc, i ->
                tr {
                    td String.format("%4d", i)
                    td String.format("%2.3f", doc.score)
                    if (doc.pmc) {
                        td { a(href:doc.url, target:'_blank', doc.pmc) }
                    }
                    else if (doc.doi) {
                        td { a(href:doc.url, target:'_blank', doc.doi.substring(0,9) + "...") }
                    }
                    else {
                        td { a(href:doc.url, target:'_blank', doc.id.substring(0,9) + "...") }
                    }
                    td doc.year
                    td doc.title.text
                }
            }
        }
    }
    div(class:'section') {
        p {
            a href:'ask', class:'plain', 'Ask another question'
        }
    }
}