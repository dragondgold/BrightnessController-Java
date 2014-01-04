package com.brightness.andres;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.DatagramSocket;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.LinkedList;
import java.util.NoSuchElementException;

public class TCPServer {

	private static final int MIN_PORT_NUMBER = 0;
	private static final int MAX_PORT_NUMBER = 65535;
	
	private int portNumber;					// Numero de puerto
	private boolean connected = false;		// Estado de la coneccion del servidor
	private boolean keepReading = false;
	
	ServerSocket mServerSocket;				// Socket del servidor
	Socket clientSocket;					// Socket del cliente
	OutputStream out;						// Streams de entrada/salida
	InputStreamReader in;

	Thread writerThread;					// Thread de lectura/escritura
	Thread readerThread;
	
	// Buffers de envío/recepción
	LinkedList<Byte> sendBuffer = new LinkedList<>();
	LinkedList<Byte> receiveBuffer = new LinkedList<>();
	
	IOnDataReceived mOnDataReceived;

	/**
	 * Comprueba si el puerto pasado existe
	 * @param port numero de puerto a comprobar
	 */
	public static boolean isPortAvailable(int port) {
	    if (port < MIN_PORT_NUMBER || port > MAX_PORT_NUMBER) {
	        throw new IllegalArgumentException("Port out of range: " + port);
	    }

	    ServerSocket ss = null;
	    DatagramSocket ds = null;
	    try {
	        ss = new ServerSocket(port);
	        ss.setReuseAddress(true);
	        ds = new DatagramSocket(port);
	        ds.setReuseAddress(true);
	        return true;
	    } catch (IOException e) {}
	    finally {
	        if (ds != null){
	        	ds.close();
	        }

	        if (ss != null) {
	            try {
	                ss.close();
	            } catch (IOException e) {}
	        }
	    }
	    return false;
	}
	
	/**
	 * Crea un server TCP
	 * @param portNumber numero de puerto donde abrir el servidor
	 */
	public TCPServer(int portNumber){
		// Abro el socket en el puerto dado y obtengo los stream
		try {
			mServerSocket = new ServerSocket(portNumber);
		} catch (IOException e) { e.printStackTrace(); }
	}
	
	public int getLocalPort(){
		return mServerSocket.getLocalPort();
	}
	
	/**
	 * Numero de puerto donde se encuentra la conexion
	 * @return
	 */
	public int getPortNumber (){
		return portNumber;
	}
	
	/**
	 * Estado de la conexion
	 * @return
	 */
	public boolean isConnected(){
		return connected;
	}
	
	/**
	 * Cierra el servidor TCP
	 */
	public void close(){
		try {
			if(mServerSocket != null) mServerSocket.close();
			if(clientSocket != null)  clientSocket.close();
		} catch (IOException e) { e.printStackTrace(); }
	}
	
	/**
	 * Queda en espera de una conexion al servidor. El método es bloqueante.
	 * @param timeout tiempo de espera máxima en mili-segundos para la conexion, 0 si no hay limite
	 * @throws SocketException tiempo de espera para la conexion agotado
	 */
	public void acceptConnection(int timeout) throws SocketException{
		mServerSocket.setSoTimeout(timeout);
		try {
			clientSocket = mServerSocket.accept();
			
			// Obtengo los stream de entrada y salida de los socket
			out = clientSocket.getOutputStream();
			in = new InputStreamReader(clientSocket.getInputStream());
		} catch (IOException e) { e.printStackTrace(); }
		
		System.out.println("Connection accepted!");
		connected = true;
		
		writerThread = new Thread(writter);
		writerThread.start();
	}
			
	/**
	 * Envía el buffer de forma asíncrona (no bloquea)
	 * @param data
	 */
	public synchronized void asyncWrite(byte[] data){
		if(connected){
			synchronized (sendBuffer) {
				for(Byte b : data)
					sendBuffer.add(b);	
			}
		}
	}
	
	/**
	 * Agrega una interface para la recepción de los datos
	 */
	public void addReceptionListener(IOnDataReceived mOnDataReceived){
		this.mOnDataReceived = mOnDataReceived;
		keepReading = true;
		
		readerThread = new Thread(receiver);
		readerThread.start();
	}
	
	public void removeReceptionListener(){
		keepReading = false;
		mOnDataReceived = null;
	}
			
	/**
	 * Thread encargado del envío de datos en el buffer
	 */
	Runnable writter = new Runnable() {
		@Override
		public void run() {
			while(true){
				try {
					synchronized(sendBuffer){
						while(sendBuffer.size() > 0){
							out.write(new byte[] { sendBuffer.removeFirst() });
						}
					}
				} catch (IOException | NoSuchElementException e) { e.printStackTrace(); }
				try { Thread.sleep(10); }
				catch (InterruptedException e) { e.printStackTrace(); }
			}
		}
	};

	/**
	 * Thread encargado de la recepcion de datos
	 */
	Runnable receiver = new Runnable() {
		@Override
		public void run() {
			try {
				while(keepReading){
					synchronized(receiveBuffer){
						byte v;
						while((v = (byte)in.read()) != -1){
							receiveBuffer.add(v);
							if(mOnDataReceived != null) mOnDataReceived.onDataReceived(receiveBuffer);
						}
					}
				}
			} catch (IOException e) { e.printStackTrace(); }
		}
	};
	
}
