async function sendQuestion() {
    const input = document.getElementById('question');
    const chat = document.getElementById('chat');
    const question = input.value.trim();
    if (!question) return;

    chat.innerHTML += `<div class="message user-message">${formatMessage(question)}</div>`;
    input.value = '';

    const response = await fetch('/chatbot/ask', {
        method: 'POST',
        headers: {'Content-Type': 'application/json'},
        body: JSON.stringify({question})
    });
    const data = await response.json();
    chat.innerHTML += `<div class="message bot-message">${formatMessage(data.answer)}</div>`;
    chat.scrollTop = chat.scrollHeight;
}

function formatMessage(text) {
    return escapeHtml(text).replace(/\n/g, '<br>');
}

function escapeHtml(text) {
    const div = document.createElement('div');
    div.innerText = text;
    return div.innerHTML;
}
