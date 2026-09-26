(function () {
  const MIRRORED_PREFIX = 'downloads/';

  function formatSize(bytes) {
    if (!bytes && bytes !== 0) return '';
    const mb = bytes / 1048576;
    return mb >= 100 ? mb.toFixed(0) + ' MB' : mb.toFixed(1) + ' MB';
  }

  function setLanguage(lang) {
    document.documentElement.lang = lang === 'en' ? 'en' : 'zh-CN';
    document.querySelectorAll('.i18n').forEach(function (el) {
      const text = el.getAttribute(lang === 'en' ? 'data-en' : 'data-zh');
      if (text) el.textContent = text;
    });
    const toggle = document.getElementById('langToggle');
    if (toggle) toggle.textContent = lang === 'en' ? '中文' : 'EN';
    try { localStorage.setItem('openvisum-lang', lang); } catch (e) {}
  }

  async function loadRelease() {
    let data;
    try {
      const response = await fetch('releases.json', { cache: 'no-store' });
      if (!response.ok) return;
      data = await response.json();
    } catch (e) {
      return;
    }
    if (!data || !data.tag) return;

    const tagEl = document.getElementById('versionTag');
    if (tagEl) tagEl.textContent = data.tag;

    const dateEl = document.getElementById('versionDate');
    if (dateEl && data.publishedAt) {
      dateEl.textContent = new Date(data.publishedAt).toISOString().slice(0, 10);
    }

    const sizes = {};
    const mirrored = data.mirrored || [];
    (data.assets || []).forEach(function (asset) {
      sizes[asset.name] = asset.size;
      const selector = '[data-size="' + asset.name + '"]';
      const sizeEl = document.querySelector(selector);
      if (sizeEl) sizeEl.textContent = formatSize(asset.size);
    });

    document.querySelectorAll('[data-asset]').forEach(function (link) {
      const name = link.getAttribute('data-asset');
      if (mirrored.indexOf(name) !== -1) {
        link.href = MIRRORED_PREFIX + name;
      } else if (data.htmlBase) {
        link.href = data.htmlBase + '/releases/download/' + encodeURIComponent(data.tag) + '/' + name;
      } else {
        link.href = 'https://github.com/Verlintas/OpenVisum/releases/latest';
      }
      link.setAttribute('download', '');
    });
  }

  document.addEventListener('DOMContentLoaded', function () {
    let stored = null;
    try { stored = localStorage.getItem('openvisum-lang'); } catch (e) {}
    const preferred = (navigator.language || '').toLowerCase().startsWith('en') ? 'en' : 'zh';
    setLanguage(stored || preferred);

    const toggle = document.getElementById('langToggle');
    if (toggle) {
      toggle.addEventListener('click', function () {
        const next = document.documentElement.lang === 'en' ? 'zh' : 'en';
        setLanguage(next);
      });
    }

    loadRelease();
  });
})();
