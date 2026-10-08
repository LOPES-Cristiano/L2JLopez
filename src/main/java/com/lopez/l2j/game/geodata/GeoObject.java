package com.lopez.l2j.game.geodata;

/**
 * Interface unificada para objetos dinamicos que alteram a Geodata em tempo real
 * (Portas de Castelos/Clanhalls, Cercas, Portoes e Barreiras Fisicas - Onda C7).
 */
public interface GeoObject {

	int geoX();

	int geoY();

	int geoZ();

	int height();

	int xMin();

	int xMax();

	int yMin();

	int yMax();

	int zMin();

	int zMax();

	/**
	 * Informa se o objeto esta atualmente bloqueando a passagem e a linha de visao (LoS).
	 *
	 * @return {@code true} se fechado/ativo/bloqueante, {@code false} se aberto/destruido/passavel
	 */
	boolean isBlocking();
}
