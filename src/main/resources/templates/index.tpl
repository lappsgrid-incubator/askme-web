layout 'layouts/main',
title: 'Example Template',
content: {
    form(action:'formAction', method:'POST') {
        input(type:'text', name: 'question', id:'question', placeholder:'Ask me a question.', required:'true', '')
        input(type:'submit', value:'Ask', '')
    }
}
