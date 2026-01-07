document.addEventListener('DOMContentLoaded', () => {
  const btn = document.getElementById('open');

  btn.addEventListener('click', () => {
    chrome.tabs.create({
      url: 'http://localhost:4200/'
    });
  });
});
