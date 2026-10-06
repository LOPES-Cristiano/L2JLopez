package com.lopez.l2j.game.extractable;

import java.util.List;

/**
 * Representa um item que pode ser aberto/extraido (bau, caixa, saco de moedas, peixe, etc.).
 */
public record ExtractableItem(int itemId, List<ExtractableProduct> products) {
}
