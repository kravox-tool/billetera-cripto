// Cryptocurrency definitions
const CRYPTO_TYPES = [
  { code: 'BTC', name: 'Bitcoin', symbol: 'BTC', network: 'Red Bitcoin (Mainnet)', emoji: '₿', color: '#F7931A' },
  { code: 'ETH', name: 'Ethereum', symbol: 'ETH', network: 'Red Ethereum / EVM', emoji: 'Ξ', color: '#627EEA' },
  { code: 'USDT_TRC20', name: 'USDT (TRC-20)', symbol: 'USDT', network: 'Red TRON (TRC-20)', emoji: '₮', color: '#26A17B' },
  { code: 'SOL', name: 'Solana', symbol: 'SOL', network: 'Red Solana (Mainnet)', emoji: '◎', color: '#9945FF' },
  { code: 'TRX', name: 'TRON', symbol: 'TRX', network: 'Red TRON', emoji: '⚡', color: '#EF0027' },
  { code: 'USDT_ERC20', name: 'USDT (ERC-20)', symbol: 'USDT', network: 'Red Ethereum (ERC-20)', emoji: '₮', color: '#26A17B' },
  { code: 'LTC', name: 'Litecoin', symbol: 'LTC', network: 'Red Litecoin', emoji: 'Ł', color: '#345D9D' },
  { code: 'DOGE', name: 'Dogecoin', symbol: 'DOGE', network: 'Red Dogecoin', emoji: 'Ð', color: '#C2A633' },
  { code: 'ADA', name: 'Cardano', symbol: 'ADA', network: 'Red Cardano (Shelley)', emoji: '₳', color: '#0033AD' },
  { code: 'TON', name: 'The Open Network', symbol: 'TON', network: 'Red TON', emoji: '💎', color: '#0098EA' },
  { code: 'XRP', name: 'Ripple', symbol: 'XRP', network: 'Red XRP Ledger', emoji: '✕', color: '#23292F' },
  { code: 'OTHER', name: 'Personalizada', symbol: 'CRYPTO', network: 'Red Desconocida', emoji: '🪙', color: '#8B5CF6' }
];

// Reference Prices fallback in USD
let currentPrices = {
  bitcoin: 67500.0,
  ethereum: 3520.0,
  solana: 155.0,
  tether: 1.0,
  tron: 0.16,
  litecoin: 72.0,
  dogecoin: 0.14,
  cardano: 0.38,
  'the-open-network': 5.80,
  ripple: 0.58
};

let currentCards = [];
let activeFilter = 'ALL';
let searchQuery = '';
let selectedCryptoCode = 'BTC';
let activeQrCard = null;

// Base58 and Bech32 helpers
const BASE58_CHARS = "123456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz";
const BECH32_CHARS = "qpzry9x8gf2tvdw0s3jn54khce6mua7l";

function isBase58(str) {
  for (let i = 0; i < str.length; i++) {
    if (!BASE58_CHARS.includes(str[i])) return false;
  }
  return true;
}

function isHex(str) {
  return /^[0-9a-fA-F]+$/.test(str);
}

// Client-side instant address detection
function detectCryptoAddress(raw) {
  const addr = (raw || '').trim();
  if (addr.length < 10) return null;

  // 1. Ethereum / EVM (0x + 40 hex)
  if (addr.startsWith('0x') && addr.length === 42 && isHex(addr.substring(2))) {
    return {
      type: 'ETH',
      name: 'Ethereum / Red EVM',
      desc: 'Dirección EVM estándar (Ethereum, BNB Chain, Polygon, Arbitrum, USDT ERC-20)',
      alternatives: ['ETH', 'USDT_ERC20']
    };
  }

  // 2. Bitcoin SegWit Native (bc1q...) or Taproot (bc1p...)
  if (addr.toLowerCase().startsWith('bc1')) {
    const isTaproot = addr.toLowerCase().startsWith('bc1p');
    return {
      type: 'BTC',
      name: 'Bitcoin (BTC)',
      desc: isTaproot ? 'Bitcoin Taproot (Bech32m)' : 'Bitcoin SegWit Nativo (Bech32)',
      alternatives: ['BTC']
    };
  }

  // 3. TRON / USDT TRC-20 (T... length 34 base58)
  if (addr.startsWith('T') && addr.length === 34 && isBase58(addr)) {
    return {
      type: 'USDT_TRC20',
      name: 'Red TRON (USDT TRC-20 / TRX)',
      desc: 'Formato estándar de TRON compatible con transferencias USDT TRC-20',
      alternatives: ['USDT_TRC20', 'TRX']
    };
  }

  // 4. Solana (Base58, length 32-44)
  if (addr.length >= 32 && addr.length <= 44 && isBase58(addr) && !addr.startsWith('0x')) {
    return {
      type: 'SOL',
      name: 'Solana (SOL)',
      desc: 'Clave pública Solana Base58',
      alternatives: ['SOL']
    };
  }

  // 5. Cardano (addr1...)
  if (addr.toLowerCase().startsWith('addr1') && addr.length >= 58) {
    return {
      type: 'ADA',
      name: 'Cardano (ADA)',
      desc: 'Dirección Shelley Cardano',
      alternatives: ['ADA']
    };
  }

  // 6. Litecoin (ltc1...)
  if (addr.toLowerCase().startsWith('ltc1')) {
    return {
      type: 'LTC',
      name: 'Litecoin (LTC)',
      desc: 'Litecoin SegWit Nativo (Bech32)',
      alternatives: ['LTC']
    };
  }

  // 7. TON (EQ... / UQ...)
  if ((addr.startsWith('EQ') || addr.startsWith('UQ')) && addr.length >= 46 && addr.length <= 50) {
    return {
      type: 'TON',
      name: 'The Open Network (TON)',
      desc: 'Dirección de usuario TON',
      alternatives: ['TON']
    };
  }

  // 8. Bitcoin Legacy (1...) or SegWit (3...)
  if ((addr.startsWith('1') || addr.startsWith('3')) && addr.length >= 26 && addr.length <= 35 && isBase58(addr)) {
    return {
      type: 'BTC',
      name: 'Bitcoin (BTC)',
      desc: addr.startsWith('1') ? 'Bitcoin Legacy (P2PKH)' : 'Bitcoin SegWit / Multi-sig (P2SH)',
      alternatives: ['BTC']
    };
  }

  // 9. Dogecoin (D...)
  if (addr.startsWith('D') && addr.length === 34 && isBase58(addr)) {
    return {
      type: 'DOGE',
      name: 'Dogecoin (DOGE)',
      desc: 'Dirección Dogecoin Legacy',
      alternatives: ['DOGE']
    };
  }

  return null;
}

// Map crypto code to price
function getUsdUnitPrice(code) {
  switch (code) {
    case 'BTC': return currentPrices.bitcoin || 67500.0;
    case 'ETH': return currentPrices.ethereum || 3520.0;
    case 'SOL': return currentPrices.solana || 155.0;
    case 'USDT_TRC20':
    case 'USDT_ERC20': return currentPrices.tether || 1.0;
    case 'TRX': return currentPrices.tron || 0.16;
    case 'LTC': return currentPrices.litecoin || 72.0;
    case 'DOGE': return currentPrices.dogecoin || 0.14;
    case 'ADA': return currentPrices.cardano || 0.38;
    case 'TON': return currentPrices['the-open-network'] || 5.80;
    case 'XRP': return currentPrices.ripple || 0.58;
    default: return 1.0;
  }
}

// UI Initialization
document.addEventListener('DOMContentLoaded', () => {
  renderCryptoSelectors();
  setupEventListeners();
  requestDataFromNative();
});

function renderCryptoSelectors() {
  const container = document.getElementById('cryptoSelector');
  container.innerHTML = '';
  CRYPTO_TYPES.forEach(item => {
    const chip = document.createElement('div');
    chip.className = `crypto-chip ${item.code === selectedCryptoCode ? 'selected' : ''}`;
    chip.dataset.code = item.code;
    chip.innerHTML = `<span>${item.emoji}</span> <span>${item.symbol}</span>`;
    chip.onclick = () => selectCrypto(item.code);
    container.appendChild(chip);
  });
}

function selectCrypto(code) {
  selectedCryptoCode = code;
  document.querySelectorAll('.crypto-chip').forEach(c => {
    c.classList.toggle('selected', c.dataset.code === code);
  });
  updateEstimatedUsd();
}

function setupEventListeners() {
  // Top Buttons
  document.getElementById('btnCreateNew').onclick = () => openCardModal();
  document.getElementById('btnCreateFirst').onclick = () => openCardModal();
  document.getElementById('btnCloseModal').onclick = () => closeModal();
  document.getElementById('btnCloseQr').onclick = () => closeQrModal();

  // Refresh
  document.getElementById('btnRefresh').onclick = () => {
    if (window.AndroidBridge && window.AndroidBridge.refreshPrices) {
      window.AndroidBridge.refreshPrices();
    }
    showToast('Actualizando cotizaciones...');
  };

  // Search
  document.getElementById('searchInput').addEventListener('input', (e) => {
    searchQuery = e.target.value.toLowerCase();
    renderCards();
  });

  // Filters
  document.querySelectorAll('.filter-pill').forEach(pill => {
    pill.onclick = () => {
      document.querySelectorAll('.filter-pill').forEach(p => p.classList.remove('active'));
      pill.classList.add('active');
      activeFilter = pill.dataset.filter;
      renderCards();
    };
  });

  // Paste Address
  document.getElementById('btnPasteAddress').onclick = async () => {
    if (window.AndroidBridge && window.AndroidBridge.getClipboardText) {
      const text = window.AndroidBridge.getClipboardText();
      if (text) {
        document.getElementById('cardAddress').value = text;
        onAddressInput(text);
      }
    } else if (navigator.clipboard && navigator.clipboard.readText) {
      try {
        const text = await navigator.clipboard.readText();
        if (text) {
          document.getElementById('cardAddress').value = text;
          onAddressInput(text);
        }
      } catch (err) {
        showToast('Pega manualmente la dirección');
      }
    }
  };

  // Address Input with real-time detection
  document.getElementById('cardAddress').addEventListener('input', (e) => {
    onAddressInput(e.target.value);
  });

  // Balance Input
  document.getElementById('cardBalance').addEventListener('input', () => {
    updateEstimatedUsd();
  });

  // QR Modal buttons
  document.getElementById('btnCopyFromQr').onclick = () => {
    if (activeQrCard) copyAddress(activeQrCard.address, activeQrCard.cryptoName);
  };
  document.getElementById('qrAddressBox').onclick = () => {
    if (activeQrCard) copyAddress(activeQrCard.address, activeQrCard.cryptoName);
  };
  document.getElementById('btnShareFromQr').onclick = () => {
    if (activeQrCard) shareAddress(activeQrCard.name, activeQrCard.networkName, activeQrCard.address);
  };
  document.getElementById('btnExplorerFromQr').onclick = () => {
    if (activeQrCard) openExplorer(activeQrCard.cryptoCode, activeQrCard.address);
  };
}

function onAddressInput(addr) {
  const result = detectCryptoAddress(addr);
  const banner = document.getElementById('detectionBanner');
  const title = document.getElementById('detectionTitleText');
  const desc = document.getElementById('detectionDescText');
  const altContainer = document.getElementById('altAssignContainer');

  if (result) {
    banner.classList.add('visible');
    title.textContent = `Detectado: ${result.name}`;
    desc.textContent = result.desc;
    selectCrypto(result.type);

    altContainer.innerHTML = '';
    if (result.alternatives && result.alternatives.length > 1) {
      result.alternatives.forEach(altCode => {
        const altDef = CRYPTO_TYPES.find(c => c.code === altCode);
        if (altDef) {
          const chip = document.createElement('div');
          chip.className = `alt-chip ${selectedCryptoCode === altCode ? 'selected' : ''}`;
          chip.textContent = `${altDef.emoji} ${altDef.symbol}`;
          chip.onclick = () => {
            selectCrypto(altCode);
            document.querySelectorAll('.alt-chip').forEach(c => c.classList.remove('selected'));
            chip.classList.add('selected');
          };
          altContainer.appendChild(chip);
        }
      });
    }
  } else {
    banner.classList.remove('visible');
  }
}

function updateEstimatedUsd() {
  const bal = parseFloat(document.getElementById('cardBalance').value) || 0;
  const label = document.getElementById('estimatedUsdLabel');
  if (bal > 0) {
    const unit = getUsdUnitPrice(selectedCryptoCode);
    const total = bal * unit;
    label.textContent = `≈ $${total.toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 })} USD`;
  } else {
    label.textContent = '';
  }
}

function openCardModal(card = null) {
  document.getElementById('cardEditId').value = card ? card.id : '';
  document.getElementById('modalTitle').textContent = card ? 'Editar Tarjeta Cripto' : 'Nueva Tarjeta Cripto';
  document.getElementById('cardName').value = card ? card.name : '';
  document.getElementById('cardAddress').value = card ? card.address : '';
  document.getElementById('cardBalance').value = (card && card.balance > 0) ? card.balance : '';
  document.getElementById('cardNotes').value = card ? (card.notes || '') : '';

  if (card) {
    selectCrypto(card.cryptoCode);
    onAddressInput(card.address);
  } else {
    selectCrypto('BTC');
    document.getElementById('detectionBanner').classList.remove('visible');
  }

  updateEstimatedUsd();
  document.getElementById('cardModal').classList.add('active');
}

function closeModal() {
  document.getElementById('cardModal').classList.remove('active');
}

function openQrModal(card) {
  activeQrCard = card;
  document.getElementById('qrCardName').textContent = card.name;
  document.getElementById('qrNetworkName').textContent = `${card.cryptoName} • ${card.networkName}`;
  document.getElementById('qrAddressVal').textContent = card.address;

  // Render QR Code onto Canvas
  renderQrToCanvas(card.address, document.getElementById('qrCanvas'));
  document.getElementById('qrModal').classList.add('active');
}

function closeQrModal() {
  document.getElementById('qrModal').classList.remove('active');
}

function saveCardForm() {
  const id = document.getElementById('cardEditId').value;
  const name = document.getElementById('cardName').value.trim();
  const address = document.getElementById('cardAddress').value.trim();
  const balance = parseFloat(document.getElementById('cardBalance').value) || 0.0;
  const notes = document.getElementById('cardNotes').value.trim();
  const cryptoDef = CRYPTO_TYPES.find(c => c.code === selectedCryptoCode) || CRYPTO_TYPES[0];

  if (!name || !address) {
    showToast('Por favor completa los campos requeridos');
    return;
  }

  const cardPayload = {
    id: id ? parseInt(id, 10) : 0,
    name,
    address,
    cryptoCode: cryptoDef.code,
    cryptoName: cryptoDef.name,
    networkName: cryptoDef.network,
    balance,
    notes,
    colorHex: parseInt(cryptoDef.color.replace('#', 'FF'), 16)
  };

  if (window.AndroidBridge && window.AndroidBridge.saveCard) {
    window.AndroidBridge.saveCard(JSON.stringify(cardPayload));
  } else {
    // Local memory fallback
    if (id) {
      const idx = currentCards.findIndex(c => c.id == id);
      if (idx !== -1) currentCards[idx] = { ...cardPayload, id: parseInt(id) };
    } else {
      cardPayload.id = Date.now();
      currentCards.unshift(cardPayload);
    }
    renderCards();
    showToast('Tarjeta guardada');
  }

  closeModal();
}

function deleteCard(id) {
  if (confirm('¿Deseas eliminar esta tarjeta?')) {
    if (window.AndroidBridge && window.AndroidBridge.deleteCard) {
      window.AndroidBridge.deleteCard(id);
    } else {
      currentCards = currentCards.filter(c => c.id != id);
      renderCards();
      showToast('Tarjeta eliminada');
    }
  }
}

function copyAddress(address, cryptoName = '') {
  if (window.AndroidBridge && window.AndroidBridge.copyToClipboard) {
    window.AndroidBridge.copyToClipboard(address);
  } else if (navigator.clipboard) {
    navigator.clipboard.writeText(address);
    showToast('Dirección copiada');
  }
}

function shareAddress(name, network, address) {
  if (window.AndroidBridge && window.AndroidBridge.shareAddress) {
    window.AndroidBridge.shareAddress(name, network, address);
  } else if (navigator.share) {
    navigator.share({
      title: name,
      text: `${name} (${network}):\n${address}`
    });
  } else {
    copyAddress(address);
  }
}

function openExplorer(cryptoCode, address) {
  if (window.AndroidBridge && window.AndroidBridge.openExplorer) {
    window.AndroidBridge.openExplorer(cryptoCode, address);
  } else {
    window.open(`https://www.blockchain.com/explorer/search?search=${address}`, '_blank');
  }
}

function showToast(msg) {
  const bar = document.getElementById('toastBar');
  bar.textContent = msg;
  bar.classList.add('visible');
  setTimeout(() => bar.classList.remove('visible'), 2400);
}

// Render Cards & Header Balance
function renderCards() {
  const emptyHero = document.getElementById('emptyHeroCard');
  const controls = document.getElementById('controlsSection');
  const cardsList = document.getElementById('cardsList');
  const totalBalText = document.getElementById('totalBalanceText');
  const totalBalInfo = document.getElementById('totalBalanceInfo');

  // Calculate total USD
  let totalUsd = 0;
  currentCards.forEach(c => {
    const unit = getUsdUnitPrice(c.cryptoCode);
    totalUsd += (c.balance || 0) * unit;
  });

  totalBalText.textContent = `$${totalUsd.toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 })} USD`;

  if (currentCards.length === 0) {
    emptyHero.style.display = 'block';
    controls.style.display = 'none';
    cardsList.innerHTML = '';
    totalBalInfo.textContent = 'No hay tarjetas creadas aún. Saldo en $0.00';
    return;
  }

  emptyHero.style.display = 'none';
  controls.style.display = 'block';
  totalBalInfo.textContent = `${currentCards.length} ${currentCards.length === 1 ? 'tarjeta guardada' : 'tarjetas guardadas'} • Cotización en tiempo real`;

  // Filter
  const filtered = currentCards.filter(card => {
    const matchesQuery = !searchQuery ||
      card.name.toLowerCase().includes(searchQuery) ||
      card.address.toLowerCase().includes(searchQuery) ||
      card.cryptoName.toLowerCase().includes(searchQuery);

    const matchesFilter = activeFilter === 'ALL' ||
      (activeFilter === 'BTC' && card.cryptoCode === 'BTC') ||
      (activeFilter === 'ETH' && (card.cryptoCode === 'ETH' || card.cryptoCode === 'USDT_ERC20')) ||
      (activeFilter === 'SOL' && card.cryptoCode === 'SOL') ||
      (activeFilter === 'TRON' && (card.cryptoCode === 'TRX' || card.cryptoCode === 'USDT_TRC20'));

    return matchesQuery && matchesFilter;
  });

  if (filtered.length === 0) {
    cardsList.innerHTML = '<div style="text-align: center; color: var(--text-muted); padding: 32px 0;">No se encontraron tarjetas</div>';
    return;
  }

  cardsList.innerHTML = '';
  filtered.forEach(card => {
    const cryptoDef = CRYPTO_TYPES.find(c => c.code === card.cryptoCode) || CRYPTO_TYPES[0];
    const unitPrice = getUsdUnitPrice(card.cryptoCode);
    const cardUsd = (card.balance || 0) * unitPrice;

    const shortAddr = card.address.length > 20
      ? `${card.address.substring(0, 10)}...${card.address.substring(card.address.length - 8)}`
      : card.address;

    const el = document.createElement('article');
    el.className = 'crypto-card';
    el.innerHTML = `
      <div class="card-top">
        <div class="card-network-group">
          <div class="network-badge" style="background: ${cryptoDef.color}26; color: ${cryptoDef.color}; border: 1px solid ${cryptoDef.color}59;">
            ${cryptoDef.emoji}
          </div>
          <div class="card-titles">
            <h3>${escapeHtml(card.name)}</h3>
            <span style="color: ${cryptoDef.color};">${card.networkName}</span>
          </div>
        </div>
        <div class="card-actions-top">
          <button class="icon-btn btn-edit" title="Editar">
            <svg width="18" height="18" viewBox="0 0 24 24" fill="currentColor">
              <path d="M3 17.25V21h3.75L17.81 9.94l-3.75-3.75L3 17.25zM20.71 7.04c.39-.39.39-1.02 0-1.41l-2.34-2.34c-.39-.39-1.02-.39-1.41 0l-1.83 1.83 3.75 3.75 1.83-1.83z"/>
            </svg>
          </button>
          <button class="icon-btn btn-delete" title="Eliminar" style="color: var(--danger-color);">
            <svg width="18" height="18" viewBox="0 0 24 24" fill="currentColor">
              <path d="M6 19c0 1.1.9 2 2 2h8c1.1 0 2-.9 2-2V7H6v12zM19 4h-3.5l-1-1h-5l-1 1H5v2h14V4z"/>
            </svg>
          </button>
        </div>
      </div>

      <div class="address-box" title="Toca para copiar">
        <div>
          <div class="address-label">DIRECCIÓN PÚBLICA</div>
          <div class="address-val">${shortAddr}</div>
        </div>
        <svg width="18" height="18" viewBox="0 0 24 24" fill="${cryptoDef.color}">
          <path d="M16 1H4c-1.1 0-2 .9-2 2v14h2V3h12V1zm3 4H8c-1.1 0-2 .9-2 2v14c0 1.1.9 2 2 2h11c1.1 0 2-.9 2-2V7c0-1.1-.9-2-2-2zm0 16H8V7h11v14z"/>
        </svg>
      </div>

      ${card.balance > 0 ? `
        <div class="balance-row">
          <div>
            <div class="bal-sub">Saldo en Billetera</div>
            <div class="bal-val">${card.balance} ${cryptoDef.symbol}</div>
          </div>
          <div>
            <div class="bal-sub" style="text-align: right;">Valor Estimado</div>
            <div class="usd-val">$${cardUsd.toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 })} USD</div>
          </div>
        </div>
      ` : ''}

      ${card.notes ? `
        <div style="font-size: 12px; color: var(--text-muted); font-style: italic; margin-bottom: 14px;">
          Nota: ${escapeHtml(card.notes)}
        </div>
      ` : ''}

      <div class="card-footer-btns">
        <button class="btn-pill-subtle btn-qr">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="currentColor">
            <path d="M3 3h8v8H3zm2 2v4h4V5zm8-2h8v8h-8zm2 2v4h4V5zM3 13h8v8H3zm2 2v4h4v-4zm13-2h3v2h-3zm-3 2h3v3h-3zm3 3h3v3h-3zm-3 3h3v2h-3zm-2-5h2v2h-2zm0 3h2v2h-2z"/>
          </svg>
          Código QR
        </button>
        <button class="btn-icon-subtle btn-share" title="Compartir">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="currentColor">
            <path d="M18 16.08c-.76 0-1.44.3-1.96.77L8.91 12.7c.05-.23.09-.46.09-.7s-.04-.47-.09-.7l7.05-4.11c.54.5 1.25.81 2.04.81 1.66 0 3-1.34 3-3s-1.34-3-3-3-3 1.34-3 3c0 .24.04.47.09.7L8.04 9.81C7.5 9.31 6.79 9 6 9c-1.66 0-3 1.34-3 3s1.34 3 3 3c.79 0 1.5-.31 2.04-.81l7.12 4.16c-.05.21-.08.43-.08.65 0 1.61 1.31 2.92 2.92 2.92 1.61 0 2.92-1.31 2.92-2.92s-1.31-2.92-2.92-2.92z"/>
          </svg>
        </button>
        <button class="btn-icon-subtle btn-explore" title="Ver en explorador de bloques">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="currentColor">
            <path d="M19 19H5V5h7V3H5c-1.11 0-2 .9-2 2v14c0 1.1.89 2 2 2h14c1.1 0 2-.9 2-2v-7h-2v7zM14 3v2h3.59l-9.83 9.83 1.41 1.41L19 6.41V10h2V3h-7z"/>
          </svg>
        </button>
      </div>
    `;

    // Hook listeners
    el.querySelector('.btn-edit').onclick = () => openCardModal(card);
    el.querySelector('.btn-delete').onclick = () => deleteCard(card.id);
    el.querySelector('.address-box').onclick = () => copyAddress(card.address, card.cryptoName);
    el.querySelector('.btn-qr').onclick = () => openQrModal(card);
    el.querySelector('.btn-share').onclick = () => shareAddress(card.name, card.networkName, card.address);
    el.querySelector('.btn-explore').onclick = () => openExplorer(card.cryptoCode, card.address);

    cardsList.appendChild(el);
  });
}

function escapeHtml(text) {
  const div = document.createElement('div');
  div.textContent = text;
  return div.innerHTML;
}

// Ultra-fast pure JavaScript QR Code Generator (Byte Mode, Version 1-7)
function renderQrToCanvas(text, canvas) {
  const matrix = generateQrMatrix(text);
  const size = matrix.length;
  canvas.width = size * 8;
  canvas.height = size * 8;
  const ctx = canvas.getContext('2d');
  ctx.fillStyle = '#ffffff';
  ctx.fillRect(0, 0, canvas.width, canvas.height);
  ctx.fillStyle = '#000000';

  const cellSize = canvas.width / size;
  for (let r = 0; r < size; r++) {
    for (let c = 0; c < size; c++) {
      if (matrix[r][c]) {
        ctx.fillRect(c * cellSize, r * cellSize, cellSize + 0.5, cellSize + 0.5);
      }
    }
  }
}

function generateQrMatrix(text) {
  // UTF-8 bytes
  const bytes = [];
  for (let i = 0; i < text.length; i++) {
    const code = text.charCodeAt(i);
    if (code < 128) bytes.push(code);
    else if (code < 2048) {
      bytes.push(192 | (code >> 6));
      bytes.push(128 | (code & 63));
    } else {
      bytes.push(224 | (code >> 12));
      bytes.push(128 | ((code >> 6) & 63));
      bytes.push(128 | (code & 63));
    }
  }

  const len = bytes.length;
  let version = 1, dataCodewords = 16, ecCodewords = 10;
  if (len <= 14) { version = 1; dataCodewords = 16; ecCodewords = 10; }
  else if (len <= 26) { version = 2; dataCodewords = 28; ecCodewords = 16; }
  else if (len <= 42) { version = 3; dataCodewords = 44; ecCodewords = 26; }
  else if (len <= 62) { version = 4; dataCodewords = 64; ecCodewords = 36; }
  else if (len <= 84) { version = 5; dataCodewords = 86; ecCodewords = 48; }
  else if (len <= 106) { version = 6; dataCodewords = 108; ecCodewords = 64; }
  else { version = 7; dataCodewords = 124; ecCodewords = 72; }

  const size = version * 4 + 17;
  const matrix = Array.from({ length: size }, () => Array(size).fill(false));
  const isFunction = Array.from({ length: size }, () => Array(size).fill(false));

  function placeFinder(top, left) {
    for (let r = -1; r <= 7; r++) {
      for (let c = -1; c <= 7; c++) {
        const mr = top + r, mc = left + c;
        if (mr >= 0 && mr < size && mc >= 0 && mc < size) {
          isFunction[mr][mc] = true;
          const isDark = (r >= 0 && r <= 6 && (c === 0 || c === 6)) ||
                         (c >= 0 && c <= 6 && (r === 0 || r === 6)) ||
                         (r >= 2 && r <= 4 && c >= 2 && c <= 4);
          matrix[mr][mc] = isDark;
        }
      }
    }
  }

  placeFinder(0, 0);
  placeFinder(0, size - 7);
  placeFinder(size - 7, 0);

  // Alignment pattern
  if (version >= 2) {
    const alignPos = [
      [], [6, 18], [6, 22], [6, 26], [6, 30], [6, 34], [6, 22, 38]
    ][version - 1] || [];
    for (const r of alignPos) {
      for (const c of alignPos) {
        if ((r <= 8 && c <= 8) || (r <= 8 && c >= size - 8) || (r >= size - 8 && c <= 8)) continue;
        for (let dr = -2; dr <= 2; dr++) {
          for (let dc = -2; dc <= 2; dc++) {
            isFunction[r + dr][c + dc] = true;
            matrix[r + dr][c + dc] = (dr === -2 || dr === 2 || dc === -2 || dc === 2 || (dr === 0 && dc === 0));
          }
        }
      }
    }
  }

  // Timing patterns
  for (let i = 8; i < size - 8; i++) {
    isFunction[6][i] = true;
    matrix[6][i] = (i % 2 === 0);
    isFunction[i][6] = true;
    matrix[i][6] = (i % 2 === 0);
  }

  // Dark module
  isFunction[size - 8][8] = true;
  matrix[size - 8][8] = true;

  // Format info reserve
  for (let i = 0; i <= 8; i++) { isFunction[8][i] = true; isFunction[i][8] = true; }
  for (let i = 0; i <= 7; i++) { isFunction[8][size - 1 - i] = true; isFunction[size - 1 - i][8] = true; }

  // Bit buffer
  const bitBuffer = [];
  function putBits(val, num) {
    for (let i = num - 1; i >= 0; i--) bitBuffer.push((val >>> i) & 1);
  }

  putBits(4, 4); // Byte mode
  putBits(len, 8); // length
  for (const b of bytes) putBits(b, 8);

  const maxDataBits = dataCodewords * 8;
  const term = Math.min(4, maxDataBits - bitBuffer.size);
  for (let i = 0; i < term; i++) bitBuffer.push(0);
  while (bitBuffer.length % 8 !== 0 && bitBuffer.length < maxDataBits) bitBuffer.push(0);

  const pads = [0xEC, 0x11];
  let pIdx = 0;
  while (bitBuffer.length < maxDataBits) {
    putBits(pads[pIdx % 2], 8);
    pIdx++;
  }

  const dataArr = [];
  for (let i = 0; i < dataCodewords; i++) {
    let b = 0;
    for (let bit = 0; bit < 8; bit++) b = (b << 1) | bitBuffer[i * 8 + bit];
    dataArr.push(b);
  }

  const ecArr = computeReedSolomon(dataArr, ecCodewords);
  const allCodewords = dataArr.concat(ecArr);
  const allBits = [];
  for (const cw of allCodewords) {
    for (let i = 7; i >= 0; i--) allBits.push((cw >>> i) & 1);
  }

  let bitIdx = 0, right = size - 1, upwards = true;
  while (right > 0) {
    if (right === 6) right--;
    const rows = upwards ? Array.from({ length: size }, (_, i) => size - 1 - i) : Array.from({ length: size }, (_, i) => i);
    for (const r of rows) {
      for (const c of [right, right - 1]) {
        if (!isFunction[r][c]) {
          const bit = bitIdx < allBits.length ? allBits[bitIdx++] : 0;
          const mask = (r + c) % 2 === 0;
          matrix[r][c] = mask ? bit === 0 : bit === 1;
        }
      }
    }
    upwards = !upwards;
    right -= 2;
  }

  // Format bits
  const formatBits = [1, 0, 1, 0, 1, 0, 0, 0, 0, 0, 1, 0, 0, 1, 0];
  const coords1 = [
    [8, 0], [8, 1], [8, 2], [8, 3], [8, 4], [8, 5],
    [8, 7], [8, 8], [7, 8], [5, 8], [4, 8], [3, 8],
    [2, 8], [1, 8], [0, 8]
  ];
  for (let i = 0; i < 15; i++) {
    const [r, c] = coords1[i];
    matrix[r][c] = formatBits[i] === 1;
  }
  for (let i = 0; i <= 6; i++) matrix[size - 1 - i][8] = formatBits[i] === 1;
  for (let i = 0; i <= 7; i++) matrix[8][size - 8 + i] = formatBits[7 + i] === 1;

  return matrix;
}

function computeReedSolomon(data, ecCount) {
  const exp = new Uint8Array(512);
  const log = new Uint8Array(256);
  let x = 1;
  for (let i = 0; i < 255; i++) {
    exp[i] = x;
    exp[i + 255] = x;
    log[x] = i;
    x = (x << 1) ^ (x >= 128 ? 0x11D : 0);
  }
  function mul(a, b) {
    if (a === 0 || b === 0) return 0;
    return exp[log[a] + log[b]];
  }

  let gen = [1];
  for (let i = 0; i < ecCount; i++) {
    const nextGen = new Array(gen.length + 1).fill(0);
    for (let j = 0; j < gen.length; j++) {
      nextGen[j] ^= mul(gen[j], exp[i]);
      nextGen[j + 1] ^= gen[j];
    }
    gen = nextGen;
  }

  const remainder = new Array(ecCount).fill(0);
  for (const byte of data) {
    const factor = byte ^ remainder[0];
    for (let j = 0; j < ecCount - 1; j++) {
      remainder[j] = remainder[j + 1] ^ mul(gen[ecCount - 1 - j], factor);
    }
    remainder[ecCount - 1] = mul(gen[0], factor);
  }
  return remainder.reverse();
}

// Bridge Integration
function requestDataFromNative() {
  if (window.AndroidBridge && window.AndroidBridge.getInitialData) {
    window.AndroidBridge.getInitialData();
  }
}

// Global Callbacks for Android Native Bridge
window.onDataUpdated = function (cardsJson, pricesJson) {
  try {
    if (cardsJson) currentCards = JSON.parse(cardsJson);
    if (pricesJson) currentPrices = { ...currentPrices, ...JSON.parse(pricesJson) };
    renderCards();
  } catch (err) {
    console.error('Error parsing native data update:', err);
  }
};

window.showToast = function (msg) {
  showToast(msg);
};

window.closeActiveModal = function () {
  const cardModal = document.getElementById('cardModal');
  const qrModal = document.getElementById('qrModal');
  if (cardModal && cardModal.classList.contains('active')) {
    cardModal.classList.remove('active');
    return true;
  }
  if (qrModal && qrModal.classList.contains('active')) {
    qrModal.classList.remove('active');
    return true;
  }
  return false;
};
