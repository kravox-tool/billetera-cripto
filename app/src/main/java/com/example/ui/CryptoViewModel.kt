package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.CryptoCardEntity
import com.example.data.CryptoRepository
import com.example.util.CryptoDetector
import com.example.util.CryptoType
import com.example.util.DetectionResult
import com.example.util.PriceService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface DialogState {
    object None : DialogState
    object Create : DialogState
    data class Edit(val card: CryptoCardEntity) : DialogState
    data class ViewQr(val card: CryptoCardEntity) : DialogState
    data class ConfirmDelete(val card: CryptoCardEntity) : DialogState
}

class CryptoViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: CryptoRepository
    init {
        val db = AppDatabase.getInstance(application)
        repository = CryptoRepository(db.cryptoCardDao())
    }

    val cards = repository.allCards.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    private val _prices = MutableStateFlow<Map<String, Double>>(emptyMap())
    val prices = _prices.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _selectedFilter = MutableStateFlow("ALL")
    val selectedFilter = _selectedFilter.asStateFlow()

    private val _dialogState = MutableStateFlow<DialogState>(DialogState.None)
    val dialogState = _dialogState.asStateFlow()

    // Form state for Create / Edit
    private val _formName = MutableStateFlow("")
    val formName = _formName.asStateFlow()

    private val _formAddress = MutableStateFlow("")
    val formAddress = _formAddress.asStateFlow()

    private val _formCryptoType = MutableStateFlow(CryptoType.BITCOIN)
    val formCryptoType = _formCryptoType.asStateFlow()

    private val _formBalance = MutableStateFlow("")
    val formBalance = _formBalance.asStateFlow()

    private val _formNotes = MutableStateFlow("")
    val formNotes = _formNotes.asStateFlow()

    private val _detectionResult = MutableStateFlow<DetectionResult?>(null)
    val detectionResult = _detectionResult.asStateFlow()

    private val _formError = MutableStateFlow<String?>(null)
    val formError = _formError.asStateFlow()

    private var editingCardId: Long? = null

    // Combined filtered cards
    val filteredCards: StateFlow<List<CryptoCardEntity>> = combine(
        cards,
        _searchQuery,
        _selectedFilter
    ) { allCards, query, filter ->
        allCards.filter { card ->
            val matchesQuery = query.isBlank() ||
                    card.name.contains(query, ignoreCase = true) ||
                    card.address.contains(query, ignoreCase = true) ||
                    card.cryptoName.contains(query, ignoreCase = true) ||
                    card.cryptoCode.contains(query, ignoreCase = true)

            val matchesFilter = when (filter) {
                "ALL" -> true
                "BTC" -> card.cryptoCode == "BTC"
                "ETH" -> card.cryptoCode in listOf("ETH", "USDT_ERC20")
                "SOL" -> card.cryptoCode == "SOL"
                "TRON" -> card.cryptoCode in listOf("TRX", "USDT_TRC20")
                else -> card.cryptoCode == filter
            }

            matchesQuery && matchesFilter
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Total USD valuation
    val totalPortfolioUsd: StateFlow<Double> = combine(cards, _prices) { cardList, _ ->
        cardList.sumOf { card ->
            val crypto = CryptoType.fromCode(card.cryptoCode)
            val unitPrice = PriceService.getUsdPriceFor(crypto)
            card.balance * unitPrice
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    init {
        refreshPrices()
    }

    fun refreshPrices() {
        viewModelScope.launch {
            val fetched = PriceService.getPrices()
            _prices.value = fetched
        }
    }

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    fun onFilterSelected(filter: String) {
        _selectedFilter.value = filter
    }

    fun openCreateDialog() {
        editingCardId = null
        _formName.value = ""
        _formAddress.value = ""
        _formCryptoType.value = CryptoType.BITCOIN
        _formBalance.value = ""
        _formNotes.value = ""
        _detectionResult.value = null
        _formError.value = null
        _dialogState.value = DialogState.Create
    }

    fun openEditDialog(card: CryptoCardEntity) {
        editingCardId = card.id
        _formName.value = card.name
        _formAddress.value = card.address
        _formCryptoType.value = CryptoType.fromCode(card.cryptoCode)
        _formBalance.value = if (card.balance > 0) card.balance.toString() else ""
        _formNotes.value = card.notes
        _detectionResult.value = CryptoDetector.detectAddress(card.address)
        _formError.value = null
        _dialogState.value = DialogState.Edit(card)
    }

    fun openQrDialog(card: CryptoCardEntity) {
        _dialogState.value = DialogState.ViewQr(card)
    }

    fun openDeleteConfirm(card: CryptoCardEntity) {
        _dialogState.value = DialogState.ConfirmDelete(card)
    }

    fun dismissDialog() {
        _dialogState.value = DialogState.None
    }

    fun onFormNameChanged(name: String) {
        _formName.value = name
        _formError.value = null
    }

    fun onFormAddressChanged(address: String) {
        _formAddress.value = address
        _formError.value = null

        // Automatic address format and network detection
        val detected = CryptoDetector.detectAddress(address)
        _detectionResult.value = detected
        if (detected != null) {
            _formCryptoType.value = detected.cryptoType
        }
    }

    fun onFormCryptoTypeChanged(type: CryptoType) {
        _formCryptoType.value = type
    }

    fun onFormBalanceChanged(balance: String) {
        // Only allow numbers and decimal point
        if (balance.isEmpty() || balance.matches(Regex("""^\d*\.?\d*$"""))) {
            _formBalance.value = balance
        }
    }

    fun onFormNotesChanged(notes: String) {
        _formNotes.value = notes
    }

    fun saveCard() {
        val name = _formName.value.trim()
        val address = _formAddress.value.trim()

        if (name.isBlank()) {
            _formError.value = "Por favor ingresa un nombre para la tarjeta"
            return
        }

        if (address.isBlank()) {
            _formError.value = "Por favor ingresa una dirección cripto"
            return
        }

        val parsedBalance = _formBalance.value.toDoubleOrNull() ?: 0.0
        val type = _formCryptoType.value

        viewModelScope.launch {
            val entity = CryptoCardEntity(
                id = editingCardId ?: 0,
                name = name,
                address = address,
                cryptoCode = type.code,
                cryptoName = type.displayName,
                networkName = type.networkName,
                balance = parsedBalance,
                notes = _formNotes.value.trim(),
                colorHex = type.colorHex
            )

            if (editingCardId == null || editingCardId == 0L) {
                repository.insertCard(entity)
            } else {
                repository.updateCard(entity)
            }

            _dialogState.value = DialogState.None
        }
    }

    fun deleteCard(card: CryptoCardEntity) {
        viewModelScope.launch {
            repository.deleteCard(card)
            _dialogState.value = DialogState.None
        }
    }
}
