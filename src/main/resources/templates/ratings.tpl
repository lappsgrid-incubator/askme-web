layout "layouts/main.gsp",
title: "LAPPS/EAGER Ratings",
version: version,
content: {
    h1 'Ratings'
    p "Size: ${data.size()}"

    table {
        tr {
            th 'UUID'
            th 'Score'
        }
        data.each { r ->
            tr {
                td r.uuid
                td r.rating
            }
        }
    }
    div {
        a(class:'btn-ok', href:'questions', 'Questions')
        a(class:'btn-ok', href:'ask', 'Ask me another')
    }
}