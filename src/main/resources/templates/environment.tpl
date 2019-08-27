layout "layouts/main.gsp",
title: "Environment"
content: {
    h1 'Environment Variables'
    table {
        tr {
            th(colspan:'2', 'Solr')
        }
        tr {
            td 'Host'
            td solr_host
        }
        tr {
            td 'Collection'
            td solr_collection
        }
    }
}