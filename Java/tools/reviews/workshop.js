(() => {
  'use strict';
  const links = [...document.querySelectorAll('nav a[data-mode]')];
  const search = document.querySelector('#search');
  const title = document.querySelector('#current-title');
  const category = document.querySelector('#category');
  const open = document.querySelector('#open');
  let frame = document.querySelector('#preview');
  let selected;

  function activate(link, restart = false) {
    if (selected === link && !restart) return;
    selected = link;
    links.forEach(item => {
      if (item === link) item.setAttribute('aria-current', 'page');
      else item.removeAttribute('aria-current');
    });
    title.textContent = link.textContent;
    category.textContent = link.closest('section').querySelector('h2').textContent;
    open.href = link.href;
    document.title = `${link.textContent} · Alderfall Preview Workshop`;
    // Discard the previous browsing context, stopping its audio and animation loops.
    // Replacing the frame also keeps child navigation out of hub mode history.
    const next = document.createElement('iframe');
    next.id = next.name = 'preview';
    next.title = link.textContent;
    next.src = link.href;
    frame.replaceWith(next);
    frame = next;
  }

  function fromHash() {
    const route = location.hash.slice(1);
    activate(links.find(link => link.dataset.mode === route) || links[0]);
  }
  links.forEach(link => link.addEventListener('click', event => {
    if (event.ctrlKey || event.metaKey || event.shiftKey || event.altKey || event.button !== 0) return;
    event.preventDefault();
    const hash = '#' + link.dataset.mode;
    if (location.hash !== hash) history.pushState(null, '', hash);
    activate(link);
  }));
  window.addEventListener('hashchange', fromHash);
  window.addEventListener('popstate', fromHash);
  document.querySelector('#reload').addEventListener('click', () => activate(selected, true));

  function filter() {
    const terms = search.value.trim().toLowerCase().split(/\s+/);
    let count = 0;
    links.forEach(link => {
      const text = `${link.textContent} ${link.closest('section').querySelector('h2').textContent}`.toLowerCase();
      link.hidden = !terms.every(term => text.includes(term));
      if (!link.hidden) count++;
    });
    document.querySelectorAll('nav section').forEach(section => {
      section.hidden = ![...section.querySelectorAll('a')].some(link => !link.hidden);
    });
    document.querySelector('#count').textContent = `${count} of ${links.length} previews`;
    document.querySelector('#empty').hidden = count !== 0;
  }
  search.addEventListener('input', filter);
  search.addEventListener('keydown', event => {
    if (event.key === 'Escape') { search.value = ''; filter(); }
    if (event.key === 'Enter') links.find(link => !link.hidden)?.click();
  });
  fromHash();
  filter();
})();
