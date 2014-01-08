package com.brightness.andres;

import java.awt.AWTException;
import java.awt.EventQueue;
import java.awt.SystemTray;
import java.awt.TrayIcon;

import javax.swing.ButtonGroup;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JSlider;
import javax.swing.UIManager;
import javax.swing.UIManager.LookAndFeelInfo;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowEvent;
import java.awt.event.WindowStateListener;

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
import java.net.SocketException;
import java.util.LinkedList;

import javax.swing.JButton;

import org.jnativehook.GlobalScreen;
import org.jnativehook.NativeHookException;
import org.jnativehook.keyboard.NativeKeyEvent;
import org.jnativehook.keyboard.NativeKeyListener;

import javax.swing.JRadioButton;

public class BrightnessChanger implements IOnDataReceived, IOnKeyEvent {

	// Keys
	private static final String SAMPLE_RATE_KEY = "sampleRate";
	private static final String PORT_NUMBER_KEY = "portNumber";
	private static final String SAMPLE_MODE_KEY = "sampleMode";
	private static final String MIN_B = "maxB";
	private static final String MAX_B = "minB";
	private static final String VERSION = "v1.0";
	
	private static final String CONFIG_FILE_NAME = "settings.xml";
	private static final int MAX_SAMPLE_RATE 	= 3000;
	private static final int MIN_SAMPLE_RATE	= 10;
	
	private static int bValue = 0;
	Settings mSettings;
	BrightnessControllerService mService;
	KeyEvent mEvent = new KeyEvent("Alt", "Tab", null, true, "Alt-Tab");
	
	private JFrame frame;
	private JTextField sampleDelayText;
	private JTextField portNumberText;
	private JSeparator separator;
	private JLabel freqLabel;
	private JButton initButton;
	private JSlider sampleRateSlider;
	private RangeSlider rangeSlider;
	private JLabel minBLabel;
	private JLabel maxBLabel;
	private JLabel bLabel;
	private JRadioButton eventSampleButton;
	private JRadioButton timeSampleButton;
	private ButtonGroup radioButtonGroup;
	
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
		    	removeHook();
		    }
		});
		
		// Cargo las configuraciones
		loadSettings();
		
		// Creo el service para controlar el brillo
		mService = new BrightnessControllerService(mSettings.getInt(PORT_NUMBER_KEY), 100);
		mEvent.addOnKeyEvent(this);
		
		// Inicio GUI
		initialize();
	}

	/**
	 * Initialize the contents of the frame.
	 */
	private void initialize() {
		frame = new JFrame();
		frame.setBounds(100, 100, 420, 238);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.getContentPane().setLayout(null);
		frame.setTitle("Controlador de brillo " + VERSION);
		frame.setResizable(false);
		frame.setIconImage(new ImageIcon("sun.png").getImage());
		
		// Si es posible al minimizar la aplicacion se minimiza al SystemTray
		if (SystemTray.isSupported()) {
            System.out.println("SystemTray soportado");
			frame.addWindowStateListener(new WindowStateListener() {
				@Override
				public void windowStateChanged(WindowEvent arg0) {
					// Minimizo la ventana
					if(arg0.getNewState() == JFrame.ICONIFIED){
						try {
							// Creo el tray icon
		                    final TrayIcon trayIcon = new TrayIcon(
		                    		new ImageIcon("sun.png").getImage(), "Controlador de brillo");
		                    trayIcon.setImageAutoSize(true);
		                    
		                    // Cuando hago click en el icono del tray muestro nuevamente la aplicacion
		                    trayIcon.addMouseListener(new MouseAdapter() {
		                        @Override
		                        public void mouseClicked(MouseEvent e) {
		                            frame.setVisible(true);
		                            frame.setState(JFrame.NORMAL);
		                            SystemTray.getSystemTray().remove(trayIcon);
		                        }
		                    });
		                    // Agrego el icono y minimizo la ventana
		                    SystemTray.getSystemTray().add(trayIcon);
		                    frame.setVisible(false);
		                } catch (AWTException e1) {
		                    e1.printStackTrace();
		                }
					}
				}
			});
		}else{
			System.out.println("SystemTray no soportado");
		}
		
		rangeSlider = new RangeSlider();
		rangeSlider.setMaximum(100);
		rangeSlider.setMinimum(0);
		rangeSlider.setBounds(10, 111, 208, 26);
		rangeSlider.setValue(mSettings.getInt(MIN_B));
		rangeSlider.setUpperValue(mSettings.getInt(MAX_B));
		frame.getContentPane().add(rangeSlider);
		
		// Cambio en el slider de niveles de brillo
		rangeSlider.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent arg0) {
				minBLabel.setText(rangeSlider.getValue() + "%");
				maxBLabel.setText(rangeSlider.getUpperValue() + "%");
				
				mSettings.setProperty(MAX_B, rangeSlider.getUpperValue());
				mSettings.setProperty(MIN_B, rangeSlider.getValue());
				saveSettings();
			}
		});
		
		sampleRateSlider = new JSlider();
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
		portNumberText.setBounds(335, 15, 55, 25);
		frame.getContentPane().add(portNumberText);
		portNumberText.setColumns(10);
		
		separator = new JSeparator();
		separator.setForeground(Color.GRAY);
		separator.setOrientation(SwingConstants.VERTICAL);
		separator.setBounds(244, 12, 2, 185);
		frame.getContentPane().add(separator);
		
		JLabel portLabel = new JLabel("Puerto:");
		portLabel.setFont(new Font("Dialog", Font.BOLD, 12));
		portLabel.setBounds(255, 19, 41, 16);
		frame.getContentPane().add(portLabel);
		
		freqLabel = new JLabel("Muestreo cada:");
		freqLabel.setFont(new Font("Dialog", Font.BOLD, 12));
		freqLabel.setBounds(15, 43, 89, 22);
		frame.getContentPane().add(freqLabel);

		initButton = new JButton("Iniciar");
		initButton.setBounds(11, 171, 98, 26);
		frame.getContentPane().add(initButton);
		initButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				// Creo el service para controlar el brillo
				mService = new BrightnessControllerService(mSettings.getInt(PORT_NUMBER_KEY), mSettings.getInt(SAMPLE_RATE_KEY));
				try {
					int r = mService.startService(true, (byte)mSettings.getInt(SAMPLE_MODE_KEY));
					if(r == -1){
						JOptionPane.showMessageDialog(frame, "Puerto no disponible");
					}else if(r == -2){
						JOptionPane.showMessageDialog(frame, "Servicio del sistema no encontrado");
					}
					else{
						mService.addOnDataReceived(BrightnessChanger.this);
						initButton.setEnabled(false);
						portNumberText.setEnabled(false);
						if(eventSampleButton.isSelected()) addHook();
						else removeHook();
					}
				} catch (SocketException e) {
					JOptionPane.showMessageDialog(frame, "Tiempo de espera para la conexion agotado");
					e.printStackTrace();
				}
			}
		});
		
		JButton stopButton = new JButton("Detener");
		stopButton.setBounds(120, 171, 98, 26);
		frame.getContentPane().add(stopButton);
		
		minBLabel = new JLabel("New label");
		minBLabel.setBounds(20, 138, 55, 16);
		minBLabel.setText(mSettings.getProperty(MIN_B) + "%");
		frame.getContentPane().add(minBLabel);
		
		maxBLabel = new JLabel("New label");
		maxBLabel.setBounds(177, 138, 41, 16);
		maxBLabel.setText(mSettings.getProperty(MAX_B) + "%");
		frame.getContentPane().add(maxBLabel);
		
		bLabel = new JLabel("Niveles de brillo");
		bLabel.setForeground(Color.BLACK);
		bLabel.setFont(new Font("Dialog", Font.BOLD, 14));
		bLabel.setBounds(56, 88, 125, 16);
		frame.getContentPane().add(bLabel);
		
		// RadioButtons para definir el tipo de muestreo
		eventSampleButton = new JRadioButton("Muestreo por evento");
		eventSampleButton.setBounds(254, 48, 166, 24);
		frame.getContentPane().add(eventSampleButton);
		eventSampleButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				System.out.println("On Demand Sample");
				mService.setSampleMode(BrightnessControllerService.ON_DEMAND_SAMPLE);
				mSettings.setProperty(SAMPLE_MODE_KEY, BrightnessControllerService.ON_DEMAND_SAMPLE);
				saveSettings();
				addHook();
			}
		});
		
		timeSampleButton = new JRadioButton("Muestreo por tiempo");
		timeSampleButton.setBounds(254, 80, 152, 24);
		frame.getContentPane().add(timeSampleButton);
		timeSampleButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				System.out.println("Timed Sample");
				mService.setSampleMode(BrightnessControllerService.TIMED_SAMPLE);
				mSettings.setProperty(SAMPLE_MODE_KEY, BrightnessControllerService.TIMED_SAMPLE);
				saveSettings();
				removeHook();
			}
		});
		
		if(mSettings.getInt(SAMPLE_MODE_KEY) == BrightnessControllerService.TIMED_SAMPLE){
			timeSampleButton.setSelected(true);
		}
		else{
			eventSampleButton.setSelected(true);
		}
		
		radioButtonGroup = new ButtonGroup();
		radioButtonGroup.add(eventSampleButton);
		radioButtonGroup.add(timeSampleButton);
		
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
			mSettings.setProperty(PORT_NUMBER_KEY, 8888);
			mSettings.setProperty(SAMPLE_RATE_KEY, 300);
			mSettings.setProperty(MIN_B, 20);
			mSettings.setProperty(MAX_B, 80);
			mSettings.setProperty(SAMPLE_MODE_KEY, BrightnessControllerService.TIMED_SAMPLE);
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
	
	private void addHook(){
		// Listener cuando se presiona alguna tecla. El listener es a nivel global asi que se reciben las letras
		//  presionadas aun cuando la aplicacion no este con focus
		try {
			GlobalScreen.registerNativeHook();
		} catch (NativeHookException e) { e.printStackTrace(); }
		GlobalScreen.getInstance().addNativeKeyListener(new NativeKeyListener() {
			
			@Override
			public void nativeKeyTyped(NativeKeyEvent arg0) {
				
			}
			
			@Override
			public void nativeKeyReleased(NativeKeyEvent arg0) {
				System.out.println("Key Released: " + NativeKeyEvent.getKeyText(arg0.getKeyCode()));
				mEvent.keyReleased(NativeKeyEvent.getKeyText(arg0.getKeyCode()));
			}
			
			@Override
			public void nativeKeyPressed(NativeKeyEvent arg0) {
				System.out.println("Key Pressed: " + NativeKeyEvent.getKeyText(arg0.getKeyCode()));
				mEvent.keyPressed(NativeKeyEvent.getKeyText(arg0.getKeyCode()));
			}
		});
		System.out.println("Hook registered");
	}
	
	private void removeHook(){
		GlobalScreen.unregisterNativeHook();
		System.out.println("Hook unregistered");
	}
	
	@Override	
	public void onDataReceived(LinkedList<Byte> receiveBuffer) {
		int receivedValue = receiveBuffer.peekLast() & 0xFF;
		int maxScreenBright = mSettings.getInt(MAX_B);
		int minScreenBright = mSettings.getInt(MIN_B);
		
		System.out.println("Received: " + receivedValue);
		
		// Calculo del brillo en pantalla
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

	@Override
	public void KeyEvent(String identifier) {
		System.out.println("Key Event: " + identifier);
		mService.doSample();
	}
}