
// import java.awt.Graphics2D;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiChannel;
import javax.sound.midi.MidiEvent;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.Sequence;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.Synthesizer;
import javax.sound.midi.Track;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.SwingUtilities;



/*
   The octave:
      pitch class     English name     French name
           0             C             do
           1             C#/Db         do diese / re bemol
           2             D             re
           3             D#/Eb         re diese / mi bemol
           4             E             mi
           5             F             fa
           6             F#/Gb         fa diese / sol bemol
           7             G             sol
           8             G#/Ab         sol diese / la bemol
           9             A             la
          10             A#/Bb         la diese / si bemol
          11             B             si
           0             C             do

   A grand piano keyboard has 88 keys:
                              Note Name     MIDI note number     Pitch class
      lowest key (1st key):       A0            21                     9
      middle C:                   C4            60                     0
      highest key (88th key):     C8           108                     0
*/


class Score {
	public static final int midiNoteNumberOfMiddleC = 60;

	public int numPitches = 88;
	public static final int pitchClassOfLowestPitch = 9; // 9==A==la
	public static final int midiNoteNumberOfLowestPitch = 21;
	public int numBeats = 128;
	public boolean [][] grid;

	public static final int numPitchesInOctave = 12;
	public String [] namesOfPitchClasses;
	public boolean [] pitchClassesInMajorScale;
	public boolean [] pitchClassesToEmphasizeInMajorScale;

	public Score() {
		grid = new boolean[ numBeats ][ numPitches ];

		namesOfPitchClasses = new String[ numPitchesInOctave ];
		namesOfPitchClasses[ 0] = "C";
		namesOfPitchClasses[ 1] = "C#";
		namesOfPitchClasses[ 2] = "D";
		namesOfPitchClasses[ 3] = "D#";
		namesOfPitchClasses[ 4] = "E";
		namesOfPitchClasses[ 5] = "F";
		namesOfPitchClasses[ 6] = "F#";
		namesOfPitchClasses[ 7] = "G";
		namesOfPitchClasses[ 8] = "G#";
		namesOfPitchClasses[ 9] = "A";
		namesOfPitchClasses[10] = "A#";
		namesOfPitchClasses[11] = "B";

		pitchClassesInMajorScale = new boolean[ numPitchesInOctave ];
		pitchClassesInMajorScale[ 0] = true;
		pitchClassesInMajorScale[ 1] = false;
		pitchClassesInMajorScale[ 2] = true;
		pitchClassesInMajorScale[ 3] = false;
		pitchClassesInMajorScale[ 4] = true;
		pitchClassesInMajorScale[ 5] = true;
		pitchClassesInMajorScale[ 6] = false;
		pitchClassesInMajorScale[ 7] = true;
		pitchClassesInMajorScale[ 8] = false;
		pitchClassesInMajorScale[ 9] = true;
		pitchClassesInMajorScale[10] = false;
		pitchClassesInMajorScale[11] = true;

		pitchClassesToEmphasizeInMajorScale = new boolean[ numPitchesInOctave ];
		pitchClassesToEmphasizeInMajorScale[ 0] = true;
		pitchClassesToEmphasizeInMajorScale[ 1] = false;
		pitchClassesToEmphasizeInMajorScale[ 2] = false;
		pitchClassesToEmphasizeInMajorScale[ 3] = false;
		pitchClassesToEmphasizeInMajorScale[ 4] = true;
		pitchClassesToEmphasizeInMajorScale[ 5] = true;
		pitchClassesToEmphasizeInMajorScale[ 6] = false;
		pitchClassesToEmphasizeInMajorScale[ 7] = true;
		pitchClassesToEmphasizeInMajorScale[ 8] = false;
		pitchClassesToEmphasizeInMajorScale[ 9] = false;
		pitchClassesToEmphasizeInMajorScale[10] = false;
		pitchClassesToEmphasizeInMajorScale[11] = false;
	}

	// returns -1 if out of bounds
	public int getMidiNoteNumberForMouseY( GraphicsWrapper gw, int mouse_y ) {
		float y = gw.convertPixelsToWorldSpaceUnitsY( mouse_y );
		int indexOfPitch = (int)(-y);
		if ( 0 <= indexOfPitch && indexOfPitch < numPitches )
			return indexOfPitch + midiNoteNumberOfLowestPitch;
		return -1;
	}

	// returns -1 if out of bounds
	public int getBeatForMouseX( GraphicsWrapper gw, int mouse_x ) {
		float x = gw.convertPixelsToWorldSpaceUnitsX( mouse_x );
		int indexOfBeat = (int)x;
		if ( 0 <= indexOfBeat && indexOfBeat < numBeats )
			return indexOfBeat;
		return -1;
	}

	public void draw(
		GraphicsWrapper gw,
		boolean highlightMajorCScale,
		int midiNoteNumber1ToHilite,
		int beat1ToHilite,
		int beat2ToHilite
	) {
		for ( int y = 0; y < numPitches; y++ ) {
			int pitchClass = ( y + pitchClassOfLowestPitch ) % numPitchesInOctave;
			int midiNoteNumber = y + midiNoteNumberOfLowestPitch;
			if ( midiNoteNumber == midiNoteNumber1ToHilite ) { // mouse cursor
				gw.setColor( 0, 1, 1 );
				gw.fillRect( 0, -y-0.8f, numBeats, 0.6f );
			}

			if ( midiNoteNumber == midiNoteNumberOfMiddleC ) {
				gw.setColor( 1, 1, 1 );
				gw.fillRect( 0, -y-0.7f, numBeats, 0.4f );
			}
			else if ( pitchClass == 0 && highlightMajorCScale ) {
				gw.setColor( 1, 1, 1 );
				gw.fillRect( 0, -y-0.6f, numBeats, 0.2f );
			}
			else if ( pitchClassesToEmphasizeInMajorScale[ pitchClass ] && highlightMajorCScale ) {
				gw.setColor( 0.6f, 0.6f, 0.6f );
				gw.fillRect( 0, -y-0.6f, numBeats, 0.2f );
			}
			else if ( pitchClassesInMajorScale[ pitchClass ] || ! highlightMajorCScale ) {
				gw.setColor( 0.6f, 0.6f, 0.6f );
				gw.fillRect( 0, -y-0.55f, numBeats, 0.1f );
			}
		}
		for ( int x = 0; x < numBeats; x++ ) {
			if ( x == beat1ToHilite ) { // mouse cursor
				gw.setColor( 0, 1, 1 );
				gw.fillRect( x+0.2f, -numPitches, 0.6f, numPitches );
			}

			if ( x == beat2ToHilite ) { // time cursor
				gw.setColor( 1, 0, 0 );
				gw.fillRect( x+0.45f, -numPitches, 0.1f, numPitches );
			}
			else if ( x % 4 == 0 ) {
				gw.setColor( 0.6f, 0.6f, 0.6f );
				gw.fillRect( x+0.45f, -numPitches, 0.1f, numPitches );
			}
		}
		gw.setColor( 0, 0, 0 );
		for ( int y = 0; y < numPitches; ++y ) {
			for ( int x = 0; x < numBeats; ++x ) {
				if ( grid[x][y] )
					gw.fillRect( x+0.3f, -y-0.7f, 0.4f, 0.4f );
			}
		}
	}

	public AlignedRectangle2D getBoundingRectangle() {
		return new AlignedRectangle2D(
			new Point2D(0,-numPitches),
			new Point2D(numBeats,0)
		);
	}

	public int getNumBeats() {
		return numBeats;
	}

	public void setNumBeats(int numBeats) {
		if (numBeats <= 128 && numBeats >= 0){
			this.numBeats = numBeats;
		}
	}
	
}

class MyCanvas extends JPanel implements KeyListener, MouseListener, MouseMotionListener, Runnable {

	SimplePianoRoll simplePianoRoll;
	GraphicsWrapper gw = new GraphicsWrapper();

	Score score = new Score();

	Metronome metronome = new Metronome();
	
	Thread thread = null;
	boolean threadSuspended;

	int currentBeat = 0;
	int sleepIntervalInMilliseconds = 150;

	public static final int RADIAL_MENU_PLAY = 0;
	public static final int RADIAL_MENU_STOP = 1;
	public static final int RADIAL_MENU_DRAW = 2;
	public static final int RADIAL_MENU_ERASE = 3;

	public static final int CONTROL_MENU_ZOOM = 0;
	public static final int CONTROL_MENU_PAN = 1;
	public static final int CONTROL_MENU_TEMPO = 2;
	public static final int CONTROL_MENU_TOTAL_DURATION = 3;
	public static final int CONTROL_MENU_TRANSPOSE = 4;

	RadialMenuWidget radialMenu = new RadialMenuWidget();
	ControlMenuWidget controlMenu = new ControlMenuWidget();

	int mouse_x, mouse_y, old_mouse_x, old_mouse_y;

	boolean isControlKeyDown = false;

	int beatOfMouseCursor = -1; // -1 for none
	int midiNoteNumberOfMouseCurser = -1; // -1 for none

	private ArrayList<String> lines;
	private Charset charset;
	private Path target;
	private File f;
	private Sequence s;

	int majorNotesTab[] = {0, 2, 4, 5, 7, 9, 11};
	int pentatonicNOtesTab[] = {0, 2, 4, 7, 9};

	public MyCanvas( SimplePianoRoll sp ) {

		target = Paths.get("notes.txt");
		charset = Charset.forName("US-ASCII");
		lines = new ArrayList<>();

		simplePianoRoll = sp;
		setBorder( BorderFactory.createLineBorder( Color.black ) );
		setBackground( Color.white );
		addKeyListener( this );
		addMouseListener( this );
		addMouseMotionListener( this );

		radialMenu.setItemLabelAndID( RadialMenuWidget.CENTRAL_ITEM, "",            RADIAL_MENU_STOP );
		radialMenu.setItemLabelAndID( 1,                             "Stop Music",  RADIAL_MENU_STOP );
		radialMenu.setItemLabelAndID( 3,                             "Draw Notes",  RADIAL_MENU_DRAW );
		radialMenu.setItemLabelAndID( 5,                             "Play Music",  RADIAL_MENU_PLAY );
		radialMenu.setItemLabelAndID( 7,                             "Erase Notes", RADIAL_MENU_ERASE );

		controlMenu.setItemLabelAndID( ControlMenuWidget.CENTRAL_ITEM, "", -1 );
		controlMenu.setItemLabelAndID( 1, "Tempo", CONTROL_MENU_TEMPO );
		controlMenu.setItemLabelAndID( 2, "Pan", CONTROL_MENU_PAN );
		controlMenu.setItemLabelAndID( 3, "Zoom", CONTROL_MENU_ZOOM );
		controlMenu.setItemLabelAndID( 5, "Total Duration", CONTROL_MENU_TOTAL_DURATION );
		controlMenu.setItemLabelAndID( 7, "Transpose", CONTROL_MENU_TRANSPOSE );

		gw.frame( score.getBoundingRectangle(), false );
	}
	public Dimension getPreferredSize() {
		return new Dimension( Constant.INITIAL_WINDOW_WIDTH, Constant.INITIAL_WINDOW_HEIGHT );
	}
	public void clear() {
		for ( int y = 0; y < score.numPitches; ++y )
			for ( int x = 0; x < score.numBeats; ++x )
				score.grid[x][y] = false;
		repaint();
	}
	public void frameAll() {
		gw.frame( score.getBoundingRectangle(), false );
		repaint();
	}
	public void paintComponent( Graphics g ) {
		super.paintComponent( g );
		gw.set( g );
		if ( getWidth() != gw.getWidth() || getHeight() != gw.getHeight() )
			gw.resize( getWidth(), getHeight() );
		gw.clear(0.4f,0.4f,0.4f);
		gw.setupForDrawing();
		gw.setCoordinateSystemToWorldSpaceUnits();
		gw.enableAlphaBlending();

		score.draw(
			gw,
			simplePianoRoll.highlightMajorScale,
			midiNoteNumberOfMouseCurser,
			beatOfMouseCursor,
			currentBeat
		);

		gw.setCoordinateSystemToPixels();

		if ( radialMenu.isVisible() )
			radialMenu.draw( gw );
		if ( controlMenu.isVisible() )
			controlMenu.draw( gw );

		if ( ! radialMenu.isVisible() && ! controlMenu.isVisible() ) {
			// draw datatip
			if ( midiNoteNumberOfMouseCurser >= 0 && beatOfMouseCursor >= 0 ) {
				final int margin = 5;
				final int x_offset = 15;

				String s = score.namesOfPitchClasses[
					( midiNoteNumberOfMouseCurser - score.midiNoteNumberOfLowestPitch + score.pitchClassOfLowestPitch )
					% score.numPitchesInOctave
				];
				int x0 = mouse_x + x_offset;
				int y0 = mouse_y - RadialMenuWidget.textHeight - 2*margin;
				int height = RadialMenuWidget.textHeight + 2*margin;
				int width = Math.round( gw.stringWidth( s ) + 2*margin );
				gw.setColor( 0, 0, 0, 0.6f );
				gw.fillRect( x0, y0, width, height );
				gw.setColor( 1, 1, 1 );
				gw.drawRect( x0, y0, width, height );
				gw.drawString( mouse_x + x_offset + margin, mouse_y - margin, s );
			}
		}
	}

	public void keyPressed( KeyEvent e ) {
		if ( e.getKeyCode() == KeyEvent.VK_CONTROL ) {
			isControlKeyDown = true;
			if (
				beatOfMouseCursor>=0
				&& simplePianoRoll.rolloverMode == SimplePianoRoll.RM_PLAY_NOTE_UPON_ROLLOVER_IF_SPECIAL_KEY_HELD_DOWN
			)
				playNote( midiNoteNumberOfMouseCurser );
		}
	}
	public void keyReleased( KeyEvent e ) {
		if ( e.getKeyCode() == KeyEvent.VK_CONTROL ) {
			isControlKeyDown = false;
			stopPlayingNote( midiNoteNumberOfMouseCurser );
		}
	}
	public void keyTyped( KeyEvent e ) {
	}


	public void mouseClicked( MouseEvent e ) { }
	public void mouseEntered( MouseEvent e ) { }
	public void mouseExited( MouseEvent e ) { }

	private void paint( int mouse_x, int mouse_y ) {
		int newBeatOfMouseCursor = score.getBeatForMouseX( gw, mouse_x );
		int newMidiNoteNumberOfMouseCurser = score.getMidiNoteNumberForMouseY( gw, mouse_y );
		if (
			newBeatOfMouseCursor != beatOfMouseCursor
			|| newMidiNoteNumberOfMouseCurser != midiNoteNumberOfMouseCurser
		) {
			beatOfMouseCursor = newBeatOfMouseCursor;
			midiNoteNumberOfMouseCurser = newMidiNoteNumberOfMouseCurser;
			repaint();
		}

		if ( beatOfMouseCursor >= 0 && midiNoteNumberOfMouseCurser >= 0 ) {
			if ( simplePianoRoll.dragMode == SimplePianoRoll.DM_DRAW_NOTES ) {
				if ( score.grid[beatOfMouseCursor][midiNoteNumberOfMouseCurser-score.midiNoteNumberOfLowestPitch] != true ) {

					int cureentNote = midiNoteNumberOfMouseCurser-score.midiNoteNumberOfLowestPitch;

					if( simplePianoRoll.isActivateMajorScale() && !isOnScale(cureentNote+Score.midiNoteNumberOfLowestPitch, Constant.MAJOR_SCALE)){
						score.grid[beatOfMouseCursor][midiNoteNumberOfMouseCurser-score.midiNoteNumberOfLowestPitch] = false;
					}
					else if( simplePianoRoll.isActivatePentatonicScale() && !isOnScale(cureentNote+Score.midiNoteNumberOfLowestPitch, Constant.PENTATONIC_SCALE) ) {
						score.grid[beatOfMouseCursor][midiNoteNumberOfMouseCurser-score.midiNoteNumberOfLowestPitch] = false;
					}
					else if( !simplePianoRoll.isActivateMajorScale() || !simplePianoRoll.isActivatePentatonicScale() ){
						score.grid[beatOfMouseCursor][midiNoteNumberOfMouseCurser-score.midiNoteNumberOfLowestPitch] = true;
					}
					System.out.println("x:"+beatOfMouseCursor+" y:"+(midiNoteNumberOfMouseCurser-score.midiNoteNumberOfLowestPitch));
					repaint();
				}
			}
			else if ( simplePianoRoll.dragMode == SimplePianoRoll.DM_ERASE_NOTES ) {
				if ( score.grid[beatOfMouseCursor][midiNoteNumberOfMouseCurser-score.midiNoteNumberOfLowestPitch] != false ) {
					score.grid[beatOfMouseCursor][midiNoteNumberOfMouseCurser-score.midiNoteNumberOfLowestPitch] = false;
					repaint();
				}
			}
		}
	}

	public void mousePressed( MouseEvent e ) {
		old_mouse_x = mouse_x;
		old_mouse_y = mouse_y;
		mouse_x = e.getX();
		mouse_y = e.getY();

		isControlKeyDown = e.isControlDown();

		if ( radialMenu.isVisible() || (SwingUtilities.isLeftMouseButton(e) && e.isControlDown()) ) {
			int returnValue = radialMenu.pressEvent( mouse_x, mouse_y );
			if ( returnValue == CustomWidget.S_REDRAW )
				repaint();
			if ( returnValue != CustomWidget.S_EVENT_NOT_CONSUMED )
				return;
		}
		if ( controlMenu.isVisible() || (SwingUtilities.isLeftMouseButton(e) && e.isShiftDown()) ) {
			int returnValue = controlMenu.pressEvent( mouse_x, mouse_y );
			if ( returnValue == CustomWidget.S_REDRAW )
				repaint();
			if ( returnValue != CustomWidget.S_EVENT_NOT_CONSUMED )
				return;
		}
		if ( SwingUtilities.isLeftMouseButton(e) ) {
			paint( mouse_x, mouse_y );
		}
	}

	public void mouseReleased( MouseEvent e ) {
		old_mouse_x = mouse_x;
		old_mouse_y = mouse_y;
		mouse_x = e.getX();
		mouse_y = e.getY();

		isControlKeyDown = e.isControlDown();

		if ( radialMenu.isVisible() ) {
			int returnValue = radialMenu.releaseEvent( mouse_x, mouse_y );

			int itemID = radialMenu.getIDOfSelection();
			if ( 0 <= itemID ) {
				switch ( itemID ) {
					case RADIAL_MENU_PLAY:
						simplePianoRoll.setMusicPlaying( true );
						break;
					case RADIAL_MENU_STOP:
						simplePianoRoll.setMusicPlaying( false );
						break;
					case RADIAL_MENU_DRAW:
						simplePianoRoll.setDragMode( SimplePianoRoll.DM_DRAW_NOTES );
						break;
					case RADIAL_MENU_ERASE:
						simplePianoRoll.setDragMode( SimplePianoRoll.DM_ERASE_NOTES );
						break;
				}
			}

			if ( returnValue == CustomWidget.S_REDRAW )
				repaint();
			if ( returnValue != CustomWidget.S_EVENT_NOT_CONSUMED )
				return;
		}
		if ( controlMenu.isVisible() ) {
			int returnValue = controlMenu.releaseEvent( mouse_x, mouse_y );

			if ( metronome.active )
				metronome.stop();
			
			if ( returnValue == CustomWidget.S_REDRAW )
				repaint();
			if ( returnValue != CustomWidget.S_EVENT_NOT_CONSUMED )
				return;
		}
	}

	private void playNote( int midiNoteNumber ) {
		if ( Constant.USE_SOUND && midiNoteNumber >= 0 ) {
			simplePianoRoll.midiChannels[0].noteOn(midiNoteNumber,Constant.midiVolume);
		}
	}
	private void stopPlayingNote( int midiNoteNumber ) {
		if ( Constant.USE_SOUND && midiNoteNumber >= 0 ) {
			simplePianoRoll.midiChannels[0].noteOff(midiNoteNumber);
		}
	}

	public void mouseMoved( MouseEvent e ) {
		old_mouse_x = mouse_x;
		old_mouse_y = mouse_y;
		mouse_x = e.getX();
		mouse_y = e.getY();

		requestFocusInWindow();

		isControlKeyDown = e.isControlDown();

		if ( radialMenu.isVisible() ) {
			int returnValue = radialMenu.moveEvent( mouse_x, mouse_y );
			if ( returnValue == CustomWidget.S_REDRAW )
				repaint();
			if ( returnValue != CustomWidget.S_EVENT_NOT_CONSUMED )
				return;
		}
		if ( controlMenu.isVisible() ) {
			int returnValue = controlMenu.moveEvent( mouse_x, mouse_y );
			if ( returnValue == CustomWidget.S_REDRAW )
				repaint();
			if ( returnValue != CustomWidget.S_EVENT_NOT_CONSUMED )
				return;
		}
		else {
			int newBeatOfMouseCursor = score.getBeatForMouseX( gw, mouse_x );
			int newMidiNoteNumberOfMouseCurser = score.getMidiNoteNumberForMouseY( gw, mouse_y );
			if ( newBeatOfMouseCursor != beatOfMouseCursor ) {
				beatOfMouseCursor = newBeatOfMouseCursor;
				repaint();
			}
			if ( newMidiNoteNumberOfMouseCurser != midiNoteNumberOfMouseCurser ) {
				stopPlayingNote( midiNoteNumberOfMouseCurser );
				midiNoteNumberOfMouseCurser = newMidiNoteNumberOfMouseCurser;
				if (
					beatOfMouseCursor>=0
					&& (
						simplePianoRoll.rolloverMode == SimplePianoRoll.RM_PLAY_NOTE_UPON_ROLLOVER
						|| (
							simplePianoRoll.rolloverMode == SimplePianoRoll.RM_PLAY_NOTE_UPON_ROLLOVER_IF_SPECIAL_KEY_HELD_DOWN
							&& isControlKeyDown
						)
					)
				)
					playNote( midiNoteNumberOfMouseCurser );
				repaint();
			}
			
		}

	}

	public void mouseDragged( MouseEvent e ) {
		old_mouse_x = mouse_x;
		old_mouse_y = mouse_y;
		mouse_x = e.getX();
		mouse_y = e.getY();
		int delta_x = mouse_x - old_mouse_x;
		int delta_y = mouse_y - old_mouse_y;

		isControlKeyDown = e.isControlDown();

		if ( radialMenu.isVisible() ) {
			int returnValue = radialMenu.dragEvent( mouse_x, mouse_y );
			if ( returnValue == CustomWidget.S_REDRAW )
				repaint();
			if ( returnValue != CustomWidget.S_EVENT_NOT_CONSUMED )
				return;
		}
		if ( controlMenu.isVisible() ) {
			if ( controlMenu.isInMenuingMode() ) {
				int returnValue = controlMenu.dragEvent( mouse_x, mouse_y );
				if ( returnValue == CustomWidget.S_REDRAW )
					repaint();
				if ( returnValue != CustomWidget.S_EVENT_NOT_CONSUMED )
					return;
			}
			else {
				// use the drag event to change the appropriate parameter
				switch ( controlMenu.getIDOfSelection() ) {
				case CONTROL_MENU_PAN:
					gw.pan( delta_x, delta_y );
					break;
				case CONTROL_MENU_ZOOM:
					gw.zoomIn( (float)Math.pow( Constant.zoomFactorPerPixelDragged, delta_x-delta_y ) );
					break;
				case CONTROL_MENU_TEMPO:
					setSleepIntervalInMilliseconds(getSleepIntervalInMilliseconds() + delta_y);
					simplePianoRoll.tempo.setText("Tempo: " + Integer.toString(getSleepIntervalInMilliseconds()) + " msec/beat");
					metronome.start(Math.round(60000/getSleepIntervalInMilliseconds()));
					break;
				case CONTROL_MENU_TOTAL_DURATION:
					score.setNumBeats(score.getNumBeats() + delta_y);
					if (simplePianoRoll.isAutoFrameActive)
						gw.frame(score.getBoundingRectangle(), true);
					break;
				default:
					// TODO XXX
					break;
				}
				repaint();
			}
		}
		else {
			paint( mouse_x, mouse_y );
		}
	}

	public void startBackgroundWork() {
		currentBeat = 0;
		if ( thread == null ) {
			thread = new Thread( this );
			threadSuspended = false;
			thread.start();
		}
		else {
			if ( threadSuspended ) {
				threadSuspended = false;
				synchronized( this ) {
					notify();
				}
			}
		}
	}
	public void stopBackgroundWork() {
		threadSuspended = true;
	}
	public void run() {
		try {
			while (true) {
				synchronized( this ) {
					if ( Constant.USE_SOUND ) {
						for ( int i = 0; i < score.numPitches; ++i ) {
							if ( score.grid[currentBeat][i] )
								simplePianoRoll.midiChannels[0].noteOff( i+score.midiNoteNumberOfLowestPitch );
						}
					}
					currentBeat += 1;
					if ( currentBeat >= score.numBeats )
						currentBeat = 0;
					if ( Constant.USE_SOUND ) {
						for ( int i = 0; i < score.numPitches; ++i ) {
							if ( score.grid[currentBeat][i] )
								simplePianoRoll.midiChannels[0].noteOn( i+score.midiNoteNumberOfLowestPitch, Constant.midiVolume );
						}
					}					
				}
				repaint();

				// Now the thread checks to see if it should suspend itself
				if ( threadSuspended ) {
					synchronized( this ) {
						while ( threadSuspended ) {
							wait();
						}
					}
				}
				thread.sleep( sleepIntervalInMilliseconds );  // interval given in milliseconds
			}
		}
		catch (InterruptedException e) { }
	}
	
	public int getSleepIntervalInMilliseconds() {
		return sleepIntervalInMilliseconds;
	}
	public void setSleepIntervalInMilliseconds(int sleepIntervalInMilliseconds) {
		if (sleepIntervalInMilliseconds > 0)
			this.sleepIntervalInMilliseconds = sleepIntervalInMilliseconds;
	}

	// save all notes
	public void saveNotes(){

		System.out.println("from save notes");
		try(BufferedWriter writer = Files.newBufferedWriter(target, charset))
		{
			String note = "";

			for(int x=0; x < score.numBeats; x++){
				for(int y=0; y<score.numPitches; y++){

					if (score.grid[x][y]){
						writer.write(note+" x: "+x+" y: "+y+"\n");
					}
				}
			}
		}catch (IOException e){
			System.out.println(e.getMessage());
		}
	}

	public void generateNotes(SimplePianoRoll spr) {

		if(spr.isActivateMajorScale()){
			generateSacle(Constant.MAJOR_SCALE);
		}else if(spr.isActivatePentatonicScale()) {
			generateSacle(Constant.PENTATONIC_SCALE); 
		}
	}

	public void generateSacle(int scaleType){

		Map range = getRange();

		int minRangeVal = (int) range.get("min");
		int maxRangeVal = (int) range.get("max");

		System.out.println("Min range: "+minRangeVal);
		System.out.println("Max range: "+maxRangeVal);

		int noteValue;
		int pitch = 0;

		noteValue = (int)  (Math.random() * (maxRangeVal - minRangeVal)) + minRangeVal;
		if( Constant.MAJOR_SCALE == scaleType ) {
			for( int x=0; x<score.numBeats; x++ ) {

				noteValue = (int)  (Math.random() * (maxRangeVal - minRangeVal)) + minRangeVal;
				System.out.println("before trs : "+noteValue);

				if( isOnScale( noteValue + Score.midiNoteNumberOfLowestPitch , Constant.MAJOR_SCALE) ){
					System.out.println("painted: "+pitch);
					// pitch =  noteValue + Score.midiNoteNumberOfLowestPitch + getYTranslation(noteValue);
					score.grid[x][noteValue] = true; //+ getYTranslation(noteValue)
				}
			}
			repaint();
		}
		else if( Constant.PENTATONIC_SCALE == scaleType ) {
			for( int x=0; x<score.numBeats; x++ ) {
 
				//noteValue += + getYTranslation(noteValue);
				noteValue = (int)  (Math.random() * (maxRangeVal - minRangeVal)) + minRangeVal;
				
				if( isOnScale( noteValue + Score.midiNoteNumberOfLowestPitch , Constant.PENTATONIC_SCALE) ){
					score.grid[x][noteValue] = true; 
				}
			}
			repaint();
		}
	}

	public int getYTranslation(int random){

		//float random = (float)Math.random();

		if( random < 45){
			return 1;
			//System.out.println(1);
		}
		else if ( random >= 45 && random <=65 ) {
			return -1;
			//System.out.println(-1);
		}
		else if( random > 70 ){
			return 0;
			//System.out.println(0);
		}
		return 1;

	}


	public Map getRange(){

		Random rand = new Random();
		Map<String, Integer> minMaxVal = new HashMap<>();

		int range =  rand.nextInt(88);

		if ( range >=3 && range <= 15 ) {
			minMaxVal.put("min", 3);
			minMaxVal.put("max", 15);
		}

		else if ( range >15 && range <= 27 ) {
			minMaxVal.put("min", 15);
			minMaxVal.put("max", 27);
		}

		else if ( range >27 && range <= 39 ) {
			minMaxVal.put("min", 27);
			minMaxVal.put("max", 39);
		}

		else if ( range >39 && range <= 51 ) {
			minMaxVal.put("min", 39);
			minMaxVal.put("max", 51);
		}

		else if ( range >51 && range <= 63 ) {
			minMaxVal.put("min", 51);
			minMaxVal.put("max", 63);
		}

		else if ( range >63 && range <= 75 ) {
			minMaxVal.put("min", 63);
			minMaxVal.put("max", 75);
		}

		else if ( range >75 && range <= 87 ) {
			minMaxVal.put("min", 75);
			minMaxVal.put("max", 87);
		}

		return minMaxVal;
	}


	public boolean isOnScale(int note, int scaleType){

		//int note_ = ( note >= 15 ) ? ( ( note % 12 ) - 3 ) : ( note % 12 );
		int note_ = note % 12;

		if( Constant.MAJOR_SCALE == scaleType ) {
			for (int val : majorNotesTab) {
				if( note_ == val ) {
					return true;
				}
			}
		}
		else if ( Constant.PENTATONIC_SCALE == scaleType ) {
			for (int val : pentatonicNOtesTab) {
				if( note_ == val ) {
					return true;
				}
			}
		}

		return false;
	}


	public void saveMidiFile(int i) {

		ShortMessage mm = new ShortMessage();
		s = null;
		try {
			s = new Sequence(javax.sound.midi.Sequence.PPQ,24);
			//mm.setMessage(0xC0, 0x00, 0x00);
			mm.setMessage(i);
			Track t = s.createTrack();
			MidiEvent me = new MidiEvent(mm,(long)121);
			t.add(me);

			mm = new ShortMessage();
			mm.setMessage(0x80,0x3C,0x40);
			me = new MidiEvent(mm,(long)121);
			t.add(me);


		} catch (InvalidMidiDataException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		f = new File("midifile.mid");

	}



	// load all saved notes
	public void loadNotes(){

		try(BufferedReader reader = Files.newBufferedReader(target, charset))
		{
			String []note;
			String tmp = "";

			while( (tmp = reader.readLine()) != null ){

				note = tmp.split("\\s+");
				System.out.println("x: "+note[2]);
				System.out.println("y: "+note[4]);

				int xNote = Integer.valueOf(note[2]);
				int yNote = Integer.valueOf(note[4]);

				score.grid[xNote][yNote] = true;
			}
		}catch(IOException e){
			System.out.println(e.getMessage());
		}
	}


	public void openNotes(){
		try (BufferedReader reader = Files.newBufferedReader(target, charset))
		{
			String line = null;
			while( (line = reader.readLine()) != null ){
				System.out.println(line);
				lines.add(line);
			}

		}catch (IOException e){
			System.out.println(e.getMessage());
		}
	}
}

public class SimplePianoRoll implements ActionListener {

	static final String applicationName = "Simple Piano Roll";

	JFrame frame;
	Container toolPanel;
	MyCanvas canvas;

	Synthesizer synthesizer;
	MidiChannel [] midiChannels;

	JMenuItem clearMenuItem;
	JMenuItem quitMenuItem;
	JCheckBoxMenuItem showToolsMenuItem;
	JCheckBoxMenuItem highlightMajorScaleMenuItem;
	JMenuItem frameAllMenuItem;
	JCheckBoxMenuItem autoFrameMenuItem;
	JMenuItem aboutMenuItem;

	JCheckBox playCheckBox;
	JCheckBox loopWhenPlayingCheckBox;

	JRadioButton drawNotesRadioButton;
	JRadioButton eraseNotesRadioButton;

	JRadioButton doNothingUponRolloverRadioButton;
	JRadioButton playNoteUponRolloverRadioButton;
	JRadioButton playNoteUponRolloverIfSpecialKeyHeldDownRadioButton;

	JRadioButton pentatonicScale;
	JRadioButton majorScale;
	JRadioButton doNothingScaleRadioButton;

	boolean activatePentatonicScale = false;
	boolean activateMajorScale = false;

	private JButton saveButton;
	private JButton loadButton;
	private JButton generateButton;

	JLabel tempo;
	
	public boolean isMusicPlaying = false;
	public boolean isMusicLoopedWhenPlayed = false;
	public boolean highlightMajorScale = true;
	public boolean isAutoFrameActive = true;

	// The DM_ prefix is for Drag Mode
	public static final int DM_DRAW_NOTES = 0;
	public static final int DM_ERASE_NOTES = 1;
	public int dragMode = DM_DRAW_NOTES;

	// The RM_ prefix is for Rollover Mode
	public static final int RM_DO_NOTHING_UPON_ROLLOVER = 0;
	public static final int RM_PLAY_NOTE_UPON_ROLLOVER = 1;
	public static final int RM_PLAY_NOTE_UPON_ROLLOVER_IF_SPECIAL_KEY_HELD_DOWN = 2;
	public int rolloverMode = RM_DO_NOTHING_UPON_ROLLOVER;


	public boolean isActivatePentatonicScale() {
		return activatePentatonicScale;
	}
	public void setActivatePentatonicScale(boolean activatePentatonicScale) {
		this.activatePentatonicScale = activatePentatonicScale;
	}
	public boolean isActivateMajorScale() {
		return activateMajorScale;
	}
	public void setActivateMajorScale(boolean activateMajorScale) {
		this.activateMajorScale = activateMajorScale;
	}

	public void setMusicPlaying( boolean flag ) {
		isMusicPlaying = flag;
		playCheckBox.setSelected( isMusicPlaying );
		if ( isMusicPlaying )
			canvas.startBackgroundWork();
		else
			canvas.stopBackgroundWork();
	}
	public void setDragMode( int newDragMode ) {
		dragMode = newDragMode;
		if ( dragMode == DM_DRAW_NOTES )
			drawNotesRadioButton.setSelected(true);
		else if ( dragMode == DM_ERASE_NOTES )
			eraseNotesRadioButton.setSelected(true);
		else assert false;
	}

	public void setRolloverMode( int newRolloverMode ) {
		rolloverMode = newRolloverMode;
		if ( rolloverMode == RM_DO_NOTHING_UPON_ROLLOVER )
			doNothingUponRolloverRadioButton.setSelected(true);
		else if ( rolloverMode == RM_PLAY_NOTE_UPON_ROLLOVER )
			playNoteUponRolloverRadioButton.setSelected(true);
		else if ( rolloverMode == RM_PLAY_NOTE_UPON_ROLLOVER_IF_SPECIAL_KEY_HELD_DOWN )
			playNoteUponRolloverIfSpecialKeyHeldDownRadioButton.setSelected(true);
		else assert  false;
	}

	public void actionPerformed(ActionEvent e) {
		Object source = e.getSource();
		if ( source == clearMenuItem ) {
			canvas.clear();
		}
		if( source == saveButton ){
			canvas.saveNotes();
		}
		else if( source == loadButton ){
			canvas.loadNotes();
		}
		else if ( source == quitMenuItem ) {
			int response = JOptionPane.showConfirmDialog(
				frame,
				"Really quit?",
				"Confirm Quit",
				JOptionPane.YES_NO_OPTION
			);

			if (response == JOptionPane.YES_OPTION) {
				System.exit(0);
			}
		}
		else if ( source == showToolsMenuItem ) {
			Container pane = frame.getContentPane();
			if ( showToolsMenuItem.isSelected() ) {
				pane.removeAll();
				pane.add( toolPanel );
				pane.add( canvas );
			}
			else {
				pane.removeAll();
				pane.add( canvas );
			}
			frame.invalidate();
			frame.validate();
		}
		else if ( source == highlightMajorScaleMenuItem ) {
			highlightMajorScale = highlightMajorScaleMenuItem.isSelected();
			canvas.repaint();
		}
		else if ( source == frameAllMenuItem ) {
			canvas.frameAll();
			canvas.repaint();
		}
		else if ( source == autoFrameMenuItem ) {
			isAutoFrameActive = autoFrameMenuItem.isSelected();
			canvas.repaint();
		}
		else if ( source == aboutMenuItem ) {
			JOptionPane.showMessageDialog(
				frame,
				"'" + applicationName + "' Sample Program\n"
					+ "Original version written April 2011",
				"About",
				JOptionPane.INFORMATION_MESSAGE
			);
		}
		else if ( source == playCheckBox ) {
			isMusicPlaying = playCheckBox.isSelected();
			if ( isMusicPlaying )
				canvas.startBackgroundWork();
			else
				canvas.stopBackgroundWork();
		}
		else if ( source == loopWhenPlayingCheckBox ) {
			isMusicLoopedWhenPlayed = loopWhenPlayingCheckBox.isSelected();
		}
		else if ( source == drawNotesRadioButton ) {
			dragMode = DM_DRAW_NOTES;
		}
		else if ( source == eraseNotesRadioButton ) {
			dragMode = DM_ERASE_NOTES;
		}
		else if ( source == doNothingUponRolloverRadioButton ) {
			rolloverMode = RM_DO_NOTHING_UPON_ROLLOVER;
		}
		else if ( source == playNoteUponRolloverRadioButton ) {
			rolloverMode = RM_PLAY_NOTE_UPON_ROLLOVER;
		}
		else if ( source == playNoteUponRolloverIfSpecialKeyHeldDownRadioButton ) {
			rolloverMode = RM_PLAY_NOTE_UPON_ROLLOVER_IF_SPECIAL_KEY_HELD_DOWN;
		}
		else if ( source == majorScale ) {
			activateMajorScale = true;
			setActivatePentatonicScale(false);
			generateButton.setEnabled(true);
		}
		else if ( source == pentatonicScale ) {
			activatePentatonicScale = true;
			setActivateMajorScale(false);
			generateButton.setEnabled(true);
		}
		else if ( source == doNothingScaleRadioButton ) {
			activatePentatonicScale = false;
			activateMajorScale = false;
			generateButton.setEnabled(false);
		}
		else if( source == generateButton){
			canvas.clear();
			canvas.generateNotes(this);
		}
	}


	// For thread safety, this should be invoked
	// from the event-dispatching thread.
	//
	private void createUI() {
		if ( Constant.USE_SOUND ) {
			try {
				synthesizer = MidiSystem.getSynthesizer();
				synthesizer.open();
				midiChannels = synthesizer.getChannels();
			}
			catch (Exception e) {
				e.printStackTrace();
			}
		}

		if ( ! SwingUtilities.isEventDispatchThread() ) {
			System.out.println(
				"Warning: UI is not being created in the Event Dispatch Thread!");
			assert false;
		}

		frame = new JFrame( applicationName );
		frame.setDefaultCloseOperation( JFrame.EXIT_ON_CLOSE );

		JMenuBar menuBar = new JMenuBar();
			JMenu menu = new JMenu("File");
				clearMenuItem = new JMenuItem("Clear");
				clearMenuItem.addActionListener(this);
				menu.add(clearMenuItem);

				menu.addSeparator();

				quitMenuItem = new JMenuItem("Quit");
				quitMenuItem.addActionListener(this);
				menu.add(quitMenuItem);
			menuBar.add(menu);
			menu = new JMenu("View");
				showToolsMenuItem = new JCheckBoxMenuItem("Show Options");
				showToolsMenuItem.setSelected( true );
				showToolsMenuItem.addActionListener(this);
				menu.add(showToolsMenuItem);

				highlightMajorScaleMenuItem = new JCheckBoxMenuItem("Highlight Major C Scale");
				highlightMajorScaleMenuItem.setSelected( highlightMajorScale );
				highlightMajorScaleMenuItem.addActionListener(this);
				menu.add(highlightMajorScaleMenuItem);

				menu.addSeparator();

				frameAllMenuItem = new JMenuItem("Frame All");
				frameAllMenuItem.addActionListener(this);
				menu.add(frameAllMenuItem);

				autoFrameMenuItem = new JCheckBoxMenuItem("Auto Frame");
				autoFrameMenuItem.setSelected( isAutoFrameActive );
				autoFrameMenuItem.addActionListener(this);
				menu.add(autoFrameMenuItem);
			menuBar.add(menu);
			menu = new JMenu("Help");
				aboutMenuItem = new JMenuItem("About");
				aboutMenuItem.addActionListener(this);
				menu.add(aboutMenuItem);
			menuBar.add(menu);
		frame.setJMenuBar(menuBar);

		toolPanel = new JPanel();
		toolPanel.setLayout( new BoxLayout( toolPanel, BoxLayout.Y_AXIS ) );

		canvas = new MyCanvas(this);

		Container pane = frame.getContentPane();
		pane.setLayout( new BoxLayout( pane, BoxLayout.X_AXIS ) );
		pane.add( toolPanel );
		pane.add( canvas );

		playCheckBox = new JCheckBox("Play", isMusicPlaying );
		playCheckBox.setAlignmentX( Component.LEFT_ALIGNMENT );
		playCheckBox.addActionListener(this);
		toolPanel.add( playCheckBox );

		loopWhenPlayingCheckBox = new JCheckBox("Loop when playing", isMusicLoopedWhenPlayed );
		loopWhenPlayingCheckBox.setAlignmentX( Component.LEFT_ALIGNMENT );
		loopWhenPlayingCheckBox.addActionListener(this);
		toolPanel.add( loopWhenPlayingCheckBox );

		toolPanel.add( Box.createRigidArea(new Dimension(1,20)) );
		toolPanel.add( new JLabel("During dragging:") );

		ButtonGroup dragModeButtonGroup = new ButtonGroup();

			drawNotesRadioButton = new JRadioButton( "Draw Notes" );
			drawNotesRadioButton.setAlignmentX( Component.LEFT_ALIGNMENT );
			drawNotesRadioButton.addActionListener(this);
			if ( dragMode == DM_DRAW_NOTES ) drawNotesRadioButton.setSelected(true);
			toolPanel.add( drawNotesRadioButton );
			dragModeButtonGroup.add( drawNotesRadioButton );

			eraseNotesRadioButton = new JRadioButton( "Erase Notes" );
			eraseNotesRadioButton.setAlignmentX( Component.LEFT_ALIGNMENT );
			eraseNotesRadioButton.addActionListener(this);
			if ( dragMode == DM_ERASE_NOTES ) eraseNotesRadioButton.setSelected(true);
			toolPanel.add( eraseNotesRadioButton );
			dragModeButtonGroup.add( eraseNotesRadioButton );

		toolPanel.add( Box.createRigidArea(new Dimension(1,20)) );
		toolPanel.add( new JLabel("Upon cursor rollover:") );

		ButtonGroup rolloverModeButtonGroup = new ButtonGroup();

			doNothingUponRolloverRadioButton = new JRadioButton( "Do Nothing" );
			doNothingUponRolloverRadioButton.setAlignmentX( Component.LEFT_ALIGNMENT );
			doNothingUponRolloverRadioButton.addActionListener(this);
			if ( rolloverMode == RM_DO_NOTHING_UPON_ROLLOVER ) doNothingUponRolloverRadioButton.setSelected(true);
			toolPanel.add( doNothingUponRolloverRadioButton );
			rolloverModeButtonGroup.add( doNothingUponRolloverRadioButton );

			playNoteUponRolloverRadioButton = new JRadioButton( "Play Pitch" );
			playNoteUponRolloverRadioButton.setAlignmentX( Component.LEFT_ALIGNMENT );
			playNoteUponRolloverRadioButton.addActionListener(this);
			if ( rolloverMode == RM_PLAY_NOTE_UPON_ROLLOVER ) playNoteUponRolloverRadioButton.setSelected(true);
			toolPanel.add( playNoteUponRolloverRadioButton );
			rolloverModeButtonGroup.add( playNoteUponRolloverRadioButton );

		playNoteUponRolloverIfSpecialKeyHeldDownRadioButton = new JRadioButton( "Play Pitch if Ctrl down" );
		playNoteUponRolloverIfSpecialKeyHeldDownRadioButton.setAlignmentX( Component.LEFT_ALIGNMENT );
		playNoteUponRolloverIfSpecialKeyHeldDownRadioButton.addActionListener(this);
		if ( rolloverMode == RM_PLAY_NOTE_UPON_ROLLOVER_IF_SPECIAL_KEY_HELD_DOWN )
			playNoteUponRolloverIfSpecialKeyHeldDownRadioButton.setSelected(true);
		toolPanel.add( playNoteUponRolloverIfSpecialKeyHeldDownRadioButton );
		rolloverModeButtonGroup.add( playNoteUponRolloverIfSpecialKeyHeldDownRadioButton );

		toolPanel.add( Box.createRigidArea(new Dimension(1,20)) );
		toolPanel.add( new JLabel("Scales:") );
		ButtonGroup scaleButtonGroup = new ButtonGroup();

		doNothingScaleRadioButton = new JRadioButton( "Do Nothing" );
		doNothingScaleRadioButton.setAlignmentX( Component.LEFT_ALIGNMENT );
		doNothingScaleRadioButton.addActionListener(this);
		if ( rolloverMode == RM_DO_NOTHING_UPON_ROLLOVER ) doNothingScaleRadioButton.setSelected(true);
		toolPanel.add( doNothingScaleRadioButton );
		scaleButtonGroup.add( doNothingScaleRadioButton );

		pentatonicScale = new JRadioButton( "Major Pentatonic" );
		pentatonicScale.setAlignmentX( Component.LEFT_ALIGNMENT );
		pentatonicScale.addActionListener(this);
		if ( rolloverMode == RM_DO_NOTHING_UPON_ROLLOVER ) pentatonicScale.setSelected(true);
		toolPanel.add( pentatonicScale );
		scaleButtonGroup.add( pentatonicScale );
		toolPanel.add( pentatonicScale );


		majorScale = new JRadioButton( "Major scale" );
		majorScale.setAlignmentX( Component.LEFT_ALIGNMENT );
		majorScale.addActionListener(this);
		if ( rolloverMode == RM_DO_NOTHING_UPON_ROLLOVER ) majorScale.setSelected(true);
		toolPanel.add( majorScale );
		scaleButtonGroup.add( majorScale );
		toolPanel.add( majorScale );

		generateButton =  new JButton("Generate");
		generateButton.addActionListener(this);
		generateButton.setEnabled(false);
		scaleButtonGroup.add(generateButton);
		toolPanel.add(generateButton);

		toolPanel.add( Box.createRigidArea(new Dimension(1,20)) );
		tempo = new JLabel("Tempo: " + canvas.getSleepIntervalInMilliseconds() + " msec/beat");
		toolPanel.add(tempo);

		toolPanel.add( Box.createRigidArea(new Dimension(1,20)) );
		toolPanel.add( new JLabel("Notes:") );

		saveButton = new JButton("Save Notes");
		saveButton.addActionListener(this);
		toolPanel.add(saveButton);

		loadButton =  new JButton("Load Notes");
		loadButton.addActionListener(this);
		toolPanel.add(loadButton);

		frame.pack();
		frame.setVisible( true );

		assert canvas.isFocusable();

	}

	public static void main( String[] args ) {
		// Schedule the creation of the UI for the event-dispatching thread.
		javax.swing.SwingUtilities.invokeLater(
			new Runnable() {
				public void run() {
					SimplePianoRoll sp = new SimplePianoRoll();
					sp.createUI();
				}
			}
		);
	}
}
