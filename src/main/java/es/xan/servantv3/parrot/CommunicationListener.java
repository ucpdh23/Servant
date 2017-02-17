package es.xan.servantv3.parrot;

public interface CommunicationListener {
	void onMessage(String sender, String message);
}
