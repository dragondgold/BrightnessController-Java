package com.brightness.andres;

import java.awt.EventQueue;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JSlider;
import javax.swing.UIManager;
import javax.swing.UIManager.LookAndFeelInfo;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JTextField;
import javax.swing.JSeparator;
import javax.swing.SwingConstants;

import java.awt.Color;

import javax.swing.JLabel;

import java.awt.Font;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.LinkedList;

import javax.swing.JButton;

public class BrightnessChanger implements IOnDataReceived {

	// Keys
	private static final String SAMPLE_RATE_KEY = "sampleRate";
	private static final String PORT_NUMBER_KEY = "portNumber";
	
	private static final String CONFIG_FILE_NAME = "settings.xml";
	private static final int MAX_SAMPLE_RATE 	= 3000;
	private static final int MIN_SAMPLE_RATE	= 10;
	
	private static int bValue = 0;
	private static int maxScreenBright = 70;
	private static int minScreenBright = 0;
	Settings mSettings;
	BrightnessControllerService mService;
	
	private JFrame frame;
	private JTextField sampleDelayText;
	private JTextField portNumberText;
	private JSeparator separator;
	private JLabel freqLabel;
	private JButton initButton;
	
	public static void main(String[] args){
		try {
			// Verifico si esta disponible el tema Nimbus
		    for (LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
		        if ("Nimbus".equals(info.getName())) {
		            UIManager.setLookAndFeel(info.getClassName());
		            break;
		        }
		    }
		} catch (Exception e) {
		    // Sino uso el por defecto de la plataforma
		    try {
		        UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
		    } catch (Exception ex) {}
		}
		
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					BrightnessChanger window = new BrightnessChanger();
					window.frame.setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

	/**
	 * Create the application.
	 */
	public BrightnessChanger() {
		// Ejecucion de operaciones cuando se cierra la aplicacion por cualquier metodo
		Runtime.getRuntime().addShutdownHook(new Thread(){
		    @Override
		    public void run(){
		    	System.out.println("Exiting");
		    	mService.stopService();
		    }
		});
		
		// Cargo las configuraciones
		loadSettings();
		
		// Creo el service para controlar el brillo
		mService = new BrightnessControllerService(mSettings.getInt(PORT_NUMBER_KEY), 100);
		
		// Inicio GUI
		initialize();
	}

	/**
	 * Initialize the contents of the frame.
	 */
	private void initialize() {
		frame = new JFrame();
		frame.setBounds(100, 100, 450, 142);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.getContentPane().setLayout(null);
		frame.setTitle("Controlador de brillo");
		frame.setResizable(false);
		
		final JSlider sampleRateSlider = new JSlider();
		sampleRateSlider.setToolTipText("");
		sampleRateSlider.setMinimum(MIN_SAMPLE_RATE);
		sampleRateSlider.setMaximum(MAX_SAMPLE_RATE);
		sampleRateSlider.setBounds(10, 11, 208, 26);
		sampleRateSlider.setValue(mSettings.getInt(SAMPLE_RATE_KEY));
		frame.getContentPane().add(sampleRateSlider);
		
		sampleDelayText = new JTextField();
		sampleDelayText.setBounds(109, 43, 77, 23);
		frame.getContentPane().add(sampleDelayText);
		sampleDelayText.setColumns(10);
		sampleDelayText.setText(sampleRateSlider.getValue() + " mS");
		
		portNumberText = new JTextField();
		portNumberText.setText(mSettings.getProperty(PORT_NUMBER_KEY));
		portNumberText.setBounds(342, 20, 55, 25);
		frame.getContentPane().add(portNumberText);
		portNumberText.setColumns(10);
		
		separator = new JSeparator();
		separator.setForeground(Color.GRAY);
		separator.setOrientation(SwingConstants.VERTICAL);
		separator.setBounds(286, 12, 2, 92);
		frame.getContentPane().add(separator);
		
		JLabel lblNewLabel = new JLabel("Puerto:");
		lblNewLabel.setFont(new Font("Dialog", Font.BOLD, 12));
		lblNewLabel.setBounds(297, 24, 55, 16);
		frame.getContentPane().add(lblNewLabel);
		
		freqLabel = new JLabel("Muestreo cada:");
		freqLabel.setFont(new Font("Dialog", Font.PLAIN, 12));
		freqLabel.setBounds(20, 43, 89, 22);
		frame.getContentPane().add(freqLabel);
		
		initButton = new JButton("Iniciar");
		initButton.setBounds(10, 78, 98, 26);
		frame.getContentPane().add(initButton);
		initButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				if(!mService.startService(true)){
					JOptionPane.showMessageDialog(frame, "Puerto no disponible");
				}
				else{
					mService.addOnDataReceived(BrightnessChanger.this);
					initButton.setEnabled(false);
					portNumberText.setEnabled(false);
				}
			}
		});
		
		JButton stopButton = new JButton("Detener");
		stopButton.setBounds(119, 78, 98, 26);
		frame.getContentPane().add(stopButton);
		stopButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				mService.stopService();
				initButton.setEnabled(true);
				portNumberText.setEnabled(true);
			}
		});
		
		// Cambio en el número de puerto
		portNumberText.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				mSettings.setProperty(PORT_NUMBER_KEY, portNumberText.getText());
				saveSettings();
			}
		});
		
		// Cambio en el slider del sample rate
		sampleRateSlider.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent arg0) {
				mSettings.setProperty(SAMPLE_RATE_KEY, sampleRateSlider.getValue());
				sampleDelayText.setText(sampleRateSlider.getValue() + " mS");
				
				mService.sendSampleRate(sampleRateSlider.getValue());
				saveSettings();
			}
		});
		
		// Cambio en el texto del sample rate
		sampleDelayText.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				String mString = sampleDelayText.getText(); 
				mString = mString.replaceAll( "[^\\d]", "" );	// Elimino todo lo que no sea numero
				
				// Verifico límites y redondeo
				int v = Integer.parseInt(mString);
				v = (v > MAX_SAMPLE_RATE) ? MAX_SAMPLE_RATE : v;
				v = (v < MIN_SAMPLE_RATE) ? MIN_SAMPLE_RATE : v;
				sampleRateSlider.setValue(v);
			}
		});
	}

	private void loadSettings(){
		mSettings = new Settings();
		
		// Si la configuración no existe (primera vez que se ejecuta el programa) escribo los valores por defecto
		File configFile = new File(CONFIG_FILE_NAME);
		if(!configFile.exists()){
			System.out.println("First time config generation");
			saveSettings();
		}
		
		// Cargo la configuración del archivo XML
		try {
			System.out.println("Loading saved config");
			mSettings.loadFromXML(new FileInputStream(CONFIG_FILE_NAME));

			System.out.println("Config loaded. sampleDelay: " + mSettings.getProperty(SAMPLE_RATE_KEY));
		} catch (IOException e) { e.printStackTrace(); }
	}

	private void saveSettings(){
		try {
			mSettings.storeToXML(new FileOutputStream(CONFIG_FILE_NAME), "Configuracion");
		} catch (IOException e) { e.printStackTrace(); }
	}

	
	@Override
	public void onDataReceived(LinkedList<Byte> receiveBuffer) {
		int receivedValue = receiveBuffer.peekLast() & 0xFF;
		
		System.out.println("Received: " + receivedValue);
		
		float m = (float)(minScreenBright - maxScreenBright) / 255f;
		int newbValue = (int)(m*receivedValue) + maxScreenBright;
		
		if(Math.abs(newbValue - bValue) > 10){
			bValue = newbValue;
		}else{
			return;
		}
		
		System.out.println("bValue: " + bValue);
		mService.sendBrightnessValue(bValue);
	}
}