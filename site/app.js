(function () {
  var MIRRORED_PREFIX = 'downloads/';
  var DOC = document.documentElement;

  /* ---------------- i18n ---------------- */
  function setLanguage(lang) {
    DOC.lang = lang === 'en' ? 'en' : 'zh-CN';
    document.querySelectorAll('.i18n').forEach(function (el) {
      var text = el.getAttribute(lang === 'en' ? 'data-en' : 'data-zh');
      if (text) el.textContent = text;
    });
    var toggle = document.getElementById('langToggle');
    if (toggle) toggle.textContent = lang === 'en' ? '中文' : 'EN';
    try { localStorage.setItem('openvisum-lang', lang); } catch (e) {}
  }

  /* ---------------- release data ---------------- */
  function formatSize(bytes) {
    if (!bytes && bytes !== 0) return '';
    var mb = bytes / 1048576;
    return mb >= 100 ? mb.toFixed(0) + ' MB' : mb.toFixed(1) + ' MB';
  }

  function escapeHtml(s) {
    return s
      .replace(/&/g, '&amp;')
      .replace(/</g, '&lt;')
      .replace(/>/g, '&gt;');
  }

  function renderNotes(markdown) {
    var lines = String(markdown || '').split('\n');
    var html = '';
    var inList = false;
    var closeList = function () {
      if (inList) { html += '</ul>'; inList = false; }
    };
    lines.forEach(function (line) {
      var text = line.trim();
      if (!text || text === '---' || text.indexOf('## ') === 0) return;
      if (text.indexOf('### ') === 0) {
        closeList();
        html += '<h5>' + inline(text.slice(4)) + '</h5>';
      } else if (/^[-*]\s/.test(text)) {
        if (!inList) { html += '<ul>'; inList = true; }
        html += '<li>' + inline(text.replace(/^[-*]\s+/, '')) + '</li>';
      } else {
        closeList();
        html += '<p>' + inline(text) + '</p>';
      }
    });
    closeList();
    return html;

    function inline(raw) {
      var safe = escapeHtml(raw);
      safe = safe.replace(/\*\*([^*]+)\*\*/g, '<strong>$1</strong>');
      safe = safe.replace(/(https?:\/\/[^\s<]+)/g, '<a href="$1" target="_blank" rel="noopener">$1</a>');
      return safe;
    }
  }

  async function loadRelease() {
    var data;
    try {
      var response = await fetch('releases.json', { cache: 'no-store' });
      if (!response.ok) return;
      data = await response.json();
    } catch (e) {
      return;
    }
    if (!data || !data.tag) return;

    ['versionTag', 'navVersion', 'releaseTag'].forEach(function (id) {
      var el = document.getElementById(id);
      if (el) el.textContent = data.tag;
    });

    var dateText = data.publishedAt ? new Date(data.publishedAt).toISOString().slice(0, 10) : '';
    var heroDate = document.getElementById('versionDate');
    if (heroDate && dateText) heroDate.textContent = dateText;
    var releaseDate = document.getElementById('releaseDate');
    if (releaseDate && dateText) releaseDate.textContent = dateText;

    (data.assets || []).forEach(function (asset) {
      var sizeEl = document.querySelector('[data-size="' + asset.name + '"]');
      if (sizeEl) sizeEl.textContent = formatSize(asset.size);
    });

    var mirrored = data.mirrored || [];
    document.querySelectorAll('[data-asset]').forEach(function (link) {
      var name = link.getAttribute('data-asset');
      if (mirrored.indexOf(name) !== -1) {
        link.href = link.href.indexOf('#') === 0 ? MIRRORED_PREFIX + name : link.href;
        if (link.closest('.dl-card')) link.href = MIRRORED_PREFIX + name;
      } else if (data.htmlBase) {
        link.href = data.htmlBase + '/releases/download/' + encodeURIComponent(data.tag) + '/' + name;
      }
    });

    var checksums = data.checksums || {};
    document.querySelectorAll('.sha[data-sha]').forEach(function (box) {
      var name = box.getAttribute('data-sha');
      var hash = checksums[name];
      var code = box.querySelector('code');
      var copy = box.querySelector('.copy');
      if (!code || !hash) return;
      code.textContent = hash.slice(0, 16) + '…';
      code.title = hash;
      if (copy) {
        copy.addEventListener('click', function () {
          var done = function () {
            var isEn = DOC.lang === 'en';
            copy.textContent = isEn ? 'Copied' : '已复制';
            setTimeout(function () {
              copy.textContent = isEn ? 'Copy' : '复制';
            }, 1400);
          };
          if (navigator.clipboard && navigator.clipboard.writeText) {
            navigator.clipboard.writeText(hash).then(done, done);
          } else {
            done();
          }
        });
      }
    });

    var notesBody = document.getElementById('releaseNotes');
    if (notesBody && data.notes) {
      notesBody.innerHTML = renderNotes(data.notes) || notesBody.innerHTML;
    }
    var releaseLink = document.getElementById('releaseLink');
    if (releaseLink && data.htmlUrl) releaseLink.href = data.htmlUrl;
  }

  /* ---------------- themes ---------------- */
  function hexToRgb(hex) {
    var value = hex.replace('#', '');
    var num = parseInt(value, 16);
    return [(num >> 16) & 255, (num >> 8) & 255, num & 255];
  }

  function applyAccent(accent, accent2) {
    if (accent === 'dynamic') {
      accent = '#4F6BED';
      accent2 = '#8FA2FF';
    }
    DOC.style.setProperty('--accent', accent);
    DOC.style.setProperty('--accent-2', accent2);
    DOC.style.setProperty('--accent-rgb', hexToRgb(accent).join(', '));
  }

  function initThemes() {
    var picker = document.getElementById('themePicker');
    if (!picker) return;
    var buttons = picker.querySelectorAll('button');
    var stored = null;
    try { stored = localStorage.getItem('openvisum-accent'); } catch (e) {}

    var activate = function (button, persist) {
      buttons.forEach(function (b) { b.classList.remove('active'); });
      button.classList.add('active');
      applyAccent(button.getAttribute('data-accent'), button.getAttribute('data-accent-2'));
      if (persist) {
        try { localStorage.setItem('openvisum-accent', button.getAttribute('data-accent')); } catch (e) {}
      }
    };

    var initial = null;
    buttons.forEach(function (b) {
      if (stored && b.getAttribute('data-accent') === stored) initial = b;
      b.addEventListener('click', function () { activate(b, true); });
    });
    if (initial) activate(initial, false);
  }

  /* ---------------- reveal ---------------- */
  function initReveal() {
    var items = document.querySelectorAll('.reveal');
    if (!('IntersectionObserver' in window)) {
      items.forEach(function (el) { el.classList.add('visible'); });
      return;
    }
    var observer = new IntersectionObserver(function (entries) {
      entries.forEach(function (entry) {
        if (entry.isIntersecting) {
          entry.target.classList.add('visible');
          observer.unobserve(entry.target);
        }
      });
    }, { rootMargin: '0px 0px -8% 0px', threshold: 0.08 });
    items.forEach(function (el) { observer.observe(el); });
  }

  /* ---------------- gallery lightbox ---------------- */
  function initLightbox() {
    var box = document.getElementById('lightbox');
    var img = document.getElementById('lbImg');
    if (!box || !img) return;
    var figures = Array.prototype.slice.call(
      document.querySelectorAll('.g-item img, .show-media img')
    );
    if (!figures.length) return;
    var index = 0;

    var show = function (i) {
      index = (i + figures.length) % figures.length;
      var source = figures[index];
      img.src = source.src;
      img.alt = source.alt || '';
      if (box.hidden) {
        box.hidden = false;
        requestAnimationFrame(function () { box.classList.add('show'); });
      }
      document.body.style.overflow = 'hidden';
    };
    var hide = function () {
      box.classList.remove('show');
      document.body.style.overflow = '';
      setTimeout(function () { box.hidden = true; }, 200);
    };

    figures.forEach(function (source, i) {
      source.parentElement.addEventListener('click', function () { show(i); });
    });
    document.getElementById('lbClose').addEventListener('click', hide);
    document.getElementById('lbPrev').addEventListener('click', function (event) { event.stopPropagation(); show(index - 1); });
    document.getElementById('lbNext').addEventListener('click', function (event) { event.stopPropagation(); show(index + 1); });
    box.addEventListener('click', function (event) { if (event.target === box) hide(); });
    document.addEventListener('keydown', function (event) {
      if (box.hidden) return;
      if (event.key === 'Escape') hide();
      if (event.key === 'ArrowLeft') show(index - 1);
      if (event.key === 'ArrowRight') show(index + 1);
    });
  }

  /* ---------------- nav ---------------- */
  function initNav() {
    var nav = document.getElementById('nav');
    var burger = document.getElementById('navBurger');
    if (nav) {
      var onScroll = function () {
        nav.classList.toggle('scrolled', window.scrollY > 8);
      };
      onScroll();
      window.addEventListener('scroll', onScroll, { passive: true });
    }
    if (burger && nav) {
      burger.addEventListener('click', function () { nav.classList.toggle('open'); });
      nav.querySelectorAll('.nav-links a').forEach(function (link) {
        link.addEventListener('click', function () { nav.classList.remove('open'); });
      });
    }
  }

  /* ---------------- boot ---------------- */
  document.addEventListener('DOMContentLoaded', function () {
    var stored = null;
    try { stored = localStorage.getItem('openvisum-lang'); } catch (e) {}
    var preferred = (navigator.language || '').toLowerCase().startsWith('en') ? 'en' : 'zh';
    setLanguage(stored || preferred);

    var toggle = document.getElementById('langToggle');
    if (toggle) {
      toggle.addEventListener('click', function () {
        setLanguage(DOC.lang === 'en' ? 'zh' : 'en');
      });
    }

    initNav();
    initThemes();
    initReveal();
    initLightbox();
    loadRelease();
  });
})();
