layout 'layouts/main.gsp',
title: 'There has been a problem',
content: {
    h1 "Apologies"
    p "Our apologies, but there seems to have been a problem processing your request."
    p "Please contact suderman@cs.vassar.edu and/or marc@cs.brandeis.edu if you wish to report a bug."
    if (message != null) {
        p message
    }
}
