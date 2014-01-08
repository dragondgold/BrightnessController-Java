package com.brightness.andres;

import java.io.IOException;
import java.net.SocketException;

public class BrightnessControllerService {

	// Constantes para comunicacion
	private static final byte CONFIG_HEADER 	= 'I';
	private static final byte SAMPLE_RATE 		= 'S';
	private static final byte BRIGHT_VALUE 		= 'B';
	private static final byte INITIAL_CONFIG 	= 'C';
	private static final byte DO_SAMPLE 		= 'D';
	private static final byte SET_SAMPLE_MODE	= 'M';
	public static final byte ON_DEMAND_SAMPLE	= 'O';
	public static final byte TIMED_SAMPLE 		= 'T';
	
	private byte sampleMode;
	
	private static final String WINDOWS_EXE		= "\"Brightness Controller-Windows.exe\"";
	
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
	 * @return -1 si no se pudo conectar al puerto, -2 si no se encontro el servicio indicado para el sistema, 0 de otro modo
	 * @throws SocketException 
	 */
	public int startService (boolean force, byte sampleMode) throws SocketException{
		
		// Si no fuerzo la conexion y ya estoy conectado retorno
		if(!force && server != null && server.isConnected()) return 0;
		else{
			
			// Compruebo disponibilidad de puerto
			if(!TCPServer.isPortAvailable(portNumber) && portNumber != 0){
				System.out.println("Puerto " + portNumber + " no disponible");
				return -1;
			}
			
			// Inicio el servidor en el puerto especificado
			System.out.println("Connecting to port " + portNumber);
			server = new TCPServer(portNumber);
			System.out.println("Connected to port " + server.getLocalPort());
			
			// Ejecuto el programa para Windows que toma las capturas de pantalla
			if(process != null) process.destroy();
			if(System.getProperty("os.name").contains("Windows")){
				Runtime rt = Runtime.getRuntime();
				try {
					process = rt.exec(WINDOWS_EXE + " " + server.getLocalPort());
				} catch (IOException e) {
					e.printStackTrace();
					return -2;
				}
			}
			
			System.out.println("Socket waiting for connection...");
			server.acceptConnection(2000);
			System.out.println("Listening in port " + portNumber);
			
			this.sampleMode = sampleMode;
			
			// Envío la primera configuracion
			server.asyncWrite(new byte[] { CONFIG_HEADER, 4, INITIAL_CONFIG, sampleMode, 
								(byte)(initialSampleDelay >> 8), (byte)(initialSampleDelay & 0xFF)});
			return 0;
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
		if(server != null)
			server.asyncWrite(new byte[] { CONFIG_HEADER, 3, SAMPLE_RATE, (byte)(sampleDelay >> 8), (byte)(sampleDelay & 0xFF)});
	}
	
	/**
	 * Configura el brillo (0 a 100%)
	 * @param value
	 */
	public void sendBrightnessValue(int value){
		if(server != null){
			if(value > 100) value = 100;
			if(value < 0) value = 0;
			server.asyncWrite(new byte[] { CONFIG_HEADER, 2, BRIGHT_VALUE, (byte)(value & 0xFF) });
		}
	}
	
	/**
	 * Agrega una interface para la recepción de los datos
	 * @param mOnDataReceived
	 */
	public void addOnDataReceived (IOnDataReceived mOnDataReceived){
		server.addReceptionListener(mOnDataReceived);
	}

	public void setSampleMode(byte sampleMode){
		this.sampleMode = sampleMode;
		if(server != null){
			server.asyncWrite(new byte[] { CONFIG_HEADER, 2, SET_SAMPLE_MODE, sampleMode});
		}
	}
	
	public byte getSampleMode() {
		return sampleMode;
	}
	
	public void doSample(){
		if(server != null){
			server.asyncWrite(new byte[] { CONFIG_HEADER, 1, DO_SAMPLE});
		}
	}
}
