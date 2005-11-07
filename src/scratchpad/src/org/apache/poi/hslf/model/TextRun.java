
/* ====================================================================
   Copyright 2002-2004   Apache Software Foundation

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
==================================================================== */
        


package org.apache.poi.hslf.model;

import java.util.LinkedList;

import org.apache.poi.hslf.record.*;
import org.apache.poi.hslf.record.StyleTextPropAtom.TextPropCollection;
import org.apache.poi.hslf.usermodel.RichTextRun;
import org.apache.poi.util.StringUtil;

/**
 * This class represents a run of text in a powerpoint document. That
 *  run could be text on a sheet, or text in a note.
 *  It is only a very basic class for now
 *
 * @author Nick Burch
 */

public class TextRun
{
	private TextHeaderAtom _headerAtom;
	private TextBytesAtom  _byteAtom;
	private TextCharsAtom  _charAtom;
	private StyleTextPropAtom _styleAtom;
	private boolean _isUnicode;
	private RichTextRun[] _rtRuns;

	/**
	* Constructs a Text Run from a Unicode text block
	*
	* @param tha the TextHeaderAtom that defines what's what
	* @param tca the TextCharsAtom containing the text
	* @param sta the StyleTextPropAtom which defines the character stylings
	*/
	public TextRun(TextHeaderAtom tha, TextCharsAtom tca, StyleTextPropAtom sta) {
		this(tha,null,tca,sta);
	}

	/**
	* Constructs a Text Run from a Ascii text block
	*
	* @param tha the TextHeaderAtom that defines what's what
	* @param tba the TextBytesAtom containing the text
	* @param sta the StyleTextPropAtom which defines the character stylings
	*/
	public TextRun(TextHeaderAtom tha, TextBytesAtom tba, StyleTextPropAtom sta) {
		this(tha,tba,null,sta);
	}
	
	/**
	 * Internal constructor and initializer
	 */
	private TextRun(TextHeaderAtom tha, TextBytesAtom tba, TextCharsAtom tca, StyleTextPropAtom sta) {
		_headerAtom = tha;
		_styleAtom = sta;
		if(tba != null) {
			_byteAtom = tba;
			_isUnicode = false;
		} else {
			_charAtom = tca;
			_isUnicode = true;
		}
		
		// Figure out the rich text runs
		// TODO: Handle when paragraph style and character styles don't match up
		LinkedList pStyles = new LinkedList();
		LinkedList cStyles = new LinkedList();
		if(_styleAtom != null) {
			pStyles = _styleAtom.getParagraphStyles();
			cStyles = _styleAtom.getCharacterStyles();
		}
		if(pStyles.size() != cStyles.size()) {
			throw new RuntimeException("Don't currently handle case of overlapping styles");
		}
		_rtRuns = new RichTextRun[pStyles.size()];
		//for(int i=0; i<)
	}
	
	
	// Update methods follow

	/**
	 * Saves the given string to the records. Doesn't touch the stylings. 
	 */
	private void storeText(String s) {
		if(_isUnicode) {
			// The atom can safely convert to unicode
			_charAtom.setText(s);
		} else {
			// Will it fit in a 8 bit atom?
			boolean hasMultibyte = StringUtil.hasMultibyte(s);
			if(! hasMultibyte) {
				// Fine to go into 8 bit atom
				byte[] text = new byte[s.length()];
				StringUtil.putCompressedUnicode(s,text,0);
				_byteAtom.setText(text);
			} else {
				throw new RuntimeException("Setting of unicode text is currently only possible for Text Runs that are Unicode in the file, sorry. For now, please convert that text to us-ascii and re-try it");
			}
		}
	}
	
	/**
	 * Handles an update to the text stored in one of the Rich Text Runs
	 * @param run
	 * @param s
	 */
	public synchronized void changeTextInRichTextRun(RichTextRun run, String s) {
		// Figure out which run it is
		int runID = -1;
		for(int i=0; i<_rtRuns.length; i++) {
			if(run.equals(_rtRuns[i])) {
				runID = i;
			}
		}
		if(runID == -1) {
			throw new IllegalArgumentException("Supplied RichTextRun wasn't from this TextRun");
		}
		
		// Ensure a StyleTextPropAtom is present, adding if required
		ensureStyleAtomPresent();
		
		// Update the text length for its Paragraph and Character stylings
		LinkedList pStyles = _styleAtom.getParagraphStyles();
		LinkedList cStyles = _styleAtom.getCharacterStyles();
		TextPropCollection pCol = (TextPropCollection)pStyles.get(runID);
		TextPropCollection cCol = (TextPropCollection)cStyles.get(runID);
		pCol.updateTextSize(s.length());
		cCol.updateTextSize(s.length());
		
		// Build up the new text
		// As we go through, update the start position for all subsequent runs
		// The building relies on the old text still being present
		StringBuffer newText = new StringBuffer();
		for(int i=0; i<_rtRuns.length; i++) {
			// Update start position
			if(i > runID) {
				_rtRuns[i].updateStartPosition(newText.length());
			}
			// Grab new text
			if(i != runID) {
				newText.append(_rtRuns[i].getRawText());
			} else {
				newText.append(s);
			}
		}
		
		// Save the new text
		storeText(newText.toString());
	}

	/**
	 * Changes the text, and sets it all to have the same styling
	 *  as the the first character has. 
	 * If you care about styling, do setText on a RichTextRun instead 
	 */
	public synchronized void setText(String s) {
		// Save the new text to the atoms
		storeText(s);

		// Now handle record stylings:
		//  everthing gets the same style that the first block has
		LinkedList pStyles = _styleAtom.getParagraphStyles();
		while(pStyles.size() > 1) { pStyles.removeLast(); }
		
		LinkedList cStyles = _styleAtom.getCharacterStyles();
		while(cStyles.size() > 1) { cStyles.removeLast(); }
		
		TextPropCollection pCol = (TextPropCollection)pStyles.getFirst();
		TextPropCollection cCol = (TextPropCollection)cStyles.getFirst();
		pCol.updateTextSize(s.length());
		cCol.updateTextSize(s.length());
		
		// Finally, zap and re-do the RichTextRuns
		_rtRuns = new RichTextRun[1];
		_rtRuns[0] = new RichTextRun(this,0,s.length());
	}

	/**
	 * Ensure a StyleTextPropAtom is present for this run, 
	 *  by adding if required
	 */
	private synchronized void ensureStyleAtomPresent() {
		if(_styleAtom != null) {
			// All there
			return;
		}
		
		// Create a new one
		_styleAtom = new StyleTextPropAtom(0);
		
		// Use the TextHeader atom to get at the parent
		RecordContainer runAtomsParent = _headerAtom.getParentRecord();
		
		// Add the new StyleTextPropAtom after the TextCharsAtom / TextBytesAtom
		Record addAfter = _byteAtom;
		if(_byteAtom == null) { addAfter = _charAtom; }
		runAtomsParent.addChildAfter(_styleAtom, addAfter);
	}

	// Accesser methods follow

	/**
	 * Returns the text content of the run, which has been made safe
	 * for printing and other use.
	 */
	public String getText() {
		String rawText = getRawText();

		// PowerPoint seems to store files with \r as the line break
		// The messes things up on everything but a Mac, so translate
		//  them to \n
		String text = rawText.replace('\r','\n');
		return text;
	}

	/**
	* Returns the raw text content of the run. This hasn't had any
	*  changes applied to it, and so is probably unlikely to print
	*  out nicely.
	*/
	public String getRawText() {
		if(_isUnicode) {
			return _charAtom.getText();
		} else {
			return _byteAtom.getText();
		}
	}
	
	/**
	 * Fetch the rich text runs (runs of text with the same styling) that
	 *  are contained within this block of text
	 * @return
	 */
	public RichTextRun[] getRichTextRuns() {
		return 	_rtRuns;
	}
	
	/**
	* Returns the type of the text, from the TextHeaderAtom.
	* Possible values can be seen from TextHeaderAtom
	* @see org.apache.poi.hslf.record.TextHeaderAtom
	*/
	public int getRunType() { 
		return _headerAtom.getTextType();
	}

	/**
	* Changes the type of the text. Values should be taken
	*  from TextHeaderAtom. No checking is done to ensure you
	*  set this to a valid value!
	* @see org.apache.poi.hslf.record.TextHeaderAtom
	*/
	public void setRunType(int type) {
		_headerAtom.setTextType(type);
	}
} 
