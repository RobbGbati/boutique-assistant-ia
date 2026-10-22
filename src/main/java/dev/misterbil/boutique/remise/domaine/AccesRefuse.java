package dev.misterbil.boutique.remise.domaine;

public class AccesRefuse extends RuntimeException {

	public AccesRefuse() {
		super("Action réservée aux employés");
	}
}
