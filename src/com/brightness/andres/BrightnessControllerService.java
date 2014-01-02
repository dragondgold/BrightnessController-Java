package com.brightness.andres;

import java.io.IOException;

public class BrightnessControllerService {

	private static final byte CONFIG_HEADER 	= 'I';
	private static final byte SAMPLE_RATE 		= 'S';
	private static final byte BRIGHT_VALUE 		= 'B';
	
	int portNumber;				// Puerto
	int initialSampleDelay;		// Delay entre cada muestreo
	
	TCPServer server = null;	// Server TCP
	Process process = null;		
	IOnDataReceived mOnDataReceived = null;
	
	/**
	 * Controlador de brillo. Se encarga de iniciar el programa correspondiente al sistema operativo.
	 * @param portNumber numero de puerto donde se realiza la conexion TCP para IPC
	 * @param initialSampleDelay delay inicial para el muestreo
	 */
	public BrightnessControllerService(int portNumber, int initialSampleDelay){
		this.portNumber = portNumber;
		this.initialSampleDelay = initialSampleDelay;
	}
	
	/**
	 * Inicia el servicio de captura de pantalla
	 * @param force forzar la reconexion
	 * @return true si se inicio correctamente, false de otro modo
	 */
	public boolean startService (boolean force){
		
		// Si no fuerzo la conexion y ya estoy conectado retorno
		if(!force && server != null && server.isConnected()) return true;
		else{
			// Compruebo disponibilidad de puerto
			if(!TCPServer.isPortAvailable(portNumber)){
				System.out.println("Puerto " + portNumber + " no disponible");
				return false;
			}
			
			// Inicio el servidor en el puerto especificado
			System.out.println("Connecting to port " + portNumber);
			server = new TCPServer(portNumber);
			
			// Ejecuto el programa para Windows que toma las capturas de pantalla
			if(process != null) process.destroy();
			if(System.getProperty("os.name").contains("Windows")){
				Runtime rt = Runtime.getRuntime();
				try {
					process = rt.exec("\"Brightness Controller.exe\" " + portNumber);
				} catch (IOException e) {
					e.printStackTrace();
					return false;
				}
			}
			
			System.out.println("Socket waiting for connection...");
			server.acceptConnection();
			System.out.println("Listening in port " + portNumber);
			
			// Envío la primera configuracion
			sendSampleRate(initialSampleDelay);
			return true;
		}
	}
	
	/**
	 * Detiene el servicio
	 */
	public void stopService(){
		if(server != null) server.close();
		if(process != null) process.destroy();
	}
	
	/**
	 * Configura el sample rate
	 * @param sampleDelay
	 */
	public void sendSampleRate(int sampleDelay){
		server.asyncWrite(new byte[] { CONFIG_HEADER, 3, SAMPLE_RATE, (byte)(sampleDelay >> 8), (byte)(sampleDelay & 0xFF)});
	}
	
	/**
	 * Configura el brillo (0 a 100%)
	 * @param value
	 */
	public void sendBrightnessValue(int value){
		if(value > 100) value = 100;
		if(value < 0) value = 0;
		server.asyncWrite(new byte[] { CONFIG_HEADER, 2, BRIGHT_VALUE, (byte)(value & 0xFF) });
	}
	
	/**
	 * Agrega una interface para la recepción de los datos
	 * @param mOnDataReceived
	 */
	public void addOnDataReceived (IOnDataReceived mOnDataReceived){
		server.addReceptionListener(mOnDataReceived);
	}
	
}
