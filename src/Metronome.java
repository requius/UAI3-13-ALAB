import java.util.logging.Level;
import java.util.logging.Logger;
import javax.sound.midi.*;

public class Metronome implements MetaEventListener {
    private Sequencer sequencer;
    private int bpm;
    
    public boolean active = false;

	public void setBpm(int bpm) {
		this.bpm = bpm;
	}

	public void start(int bpm) {
        try {
        	setBpm(bpm);
        	if (!active){
        		active = true;
        		sequencer = MidiSystem.getSequencer();
                sequencer.open();
                sequencer.addMetaEventListener(this);
                
	            Sequence seq = createSequence();
	            sequencer.setSequence(seq);
	            sequencer.setTempoInBPM(bpm);
	            sequencer.start();
        	}
        } catch (InvalidMidiDataException | MidiUnavailableException e) {
            Logger.getLogger(Metronome.class.getName()).log(Level.SEVERE, null, e);
        }
    }
	
	public void stop(){
		active = false;
		sequencer.stop();
	}

    private Sequence createSequence() {
        try {
            Sequence seq = new Sequence(Sequence.PPQ, 1);
            Track track = seq.createTrack();

            ShortMessage msg = new ShortMessage(ShortMessage.PROGRAM_CHANGE, 9, 1, 0);
            MidiEvent evt = new MidiEvent(msg, 0);
            track.add(evt);

            ShortMessage message = new ShortMessage(ShortMessage.NOTE_ON, 9, 37, 100);
            MidiEvent event = new MidiEvent(message, 0);
            track.add(event);

            msg = new ShortMessage(ShortMessage.PROGRAM_CHANGE, 9, 1, 0);
            evt = new MidiEvent(msg, 1);
            track.add(evt);
            
            return seq;
        } catch (InvalidMidiDataException e) {
            Logger.getLogger(Metronome.class.getName()).log(Level.SEVERE, null, e);
            return null;
        }
    }

    public void meta(MetaMessage message) {
        if (message.getType() != 47) {
            return;
        }
        doLoop();
    }

    private void doLoop() {
        if (sequencer == null || !sequencer.isOpen()) {
            return;
        }
        sequencer.setTickPosition(0);
        sequencer.start();
        sequencer.setTempoInBPM(bpm);
    }
}