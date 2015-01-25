/*********************************************
 * Copyright (c) 2011 LeanPulse.
 * All rights reserved.
 *********************************************/
package com.leanpulse.bibant.pdf;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.pdfbox.cos.COSArray;
import org.apache.pdfbox.cos.COSBase;
import org.apache.pdfbox.cos.COSInteger;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.cos.COSNumber;
import org.apache.pdfbox.cos.COSObject;
import org.apache.pdfbox.cos.COSString;
import org.apache.pdfbox.encoding.Encoding;
import org.apache.pdfbox.pdfparser.PDFStreamParser;
import org.apache.pdfbox.pdfwriter.ContentStreamWriter;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDStream;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.util.MapUtil;
import org.apache.pdfbox.util.PDFOperator;

/**
 * Replace substrings of text elements in a PDF document that match the given
 * regular expressions with the given replacements.
 * 
 * <p>
 * To fasten the process, a count per page and a range of pages can be given for
 * each replace entry.
 * 
 * <dl>
 * <dt><b>Restrictions:</b></dt>
 * <dd>
 * - PDF is a graphic language and this class can only match strings in
 * contiguous text elements.<br>
 * - Some text elements may be split in arrays of strings with in-between
 * position offsets. This class enables to replace substrings in such elements
 * but will remove the in-between offsets.<br>
 * - PDF can embed a subset of glyphs when special fonts are used. This class
 * can decode text using such fonts to check if an element matches but can't
 * encode the replace string. As a consequence, in such case, only an empty
 * replacement can be provided (to remove the matched substrings).
 * </dl>
 * 
 * @author <a href="mailto:a.prost@leanpulse.com">Aurélien PROST</a>
 */
public class ReplaceRegExp implements PdfProcessor {
	
	private static final Log log = LogFactory.getLog(ReplaceRegExp.class);

	private static class ReplaceEntry {
		private Pattern regexp;
		private String replacement;
		public int countperpage = -1;
		public int startpage = -1;
		public int endpage = Integer.MAX_VALUE;

		public ReplaceEntry(String regexp, String replacement) {
			this(regexp, replacement, -1, -1, Integer.MAX_VALUE);
		}

		public ReplaceEntry(String regexp, String replacement, int countperpage) {
			this(regexp, replacement, countperpage, -1, Integer.MAX_VALUE);
		}

		public ReplaceEntry(String regexp, String replacement,
				int countperpage, int startpage, int endpage) {
			this.regexp = Pattern.compile(regexp);
			this.replacement = replacement;
			this.countperpage = countperpage;
			this.startpage = startpage;
			this.endpage = endpage;
		}

		public boolean matches(String text) {
			Matcher m = regexp.matcher(text);
			return m.matches();
		}

		public String replace(String text) {
			Matcher m = regexp.matcher(text);
			return m.replaceAll(replacement);
		}

		public int hashCode() {
			return regexp.hashCode();
		}
	}

	private PDFont subFont;
	private Set<ReplaceEntry> searchEntries = new HashSet<ReplaceEntry>();
	private List<ReplaceEntry> curPagesearchEntries;
	
	public ReplaceRegExp(PDFont defaultFont) {
		this.subFont = defaultFont;
	}

	/**
	 * Add a new entry to replace all substrings of text elements that match the
	 * given regular expression with the given replacement, in the entire PDF
	 * document.
	 * 
	 * @param regexp
	 *            The regular expression to search,
	 * @param replacement
	 *            The replacement string.
	 */
	public void addReplace(String regexp, String replacement) {
		searchEntries.add(new ReplaceEntry(regexp, replacement));
	}

	/**
	 * Add a new entry to replace all substrings of text elements that are the
	 * first per page to match the given regular expression with the given
	 * replacement, in the entire PDF document.
	 * 
	 * @param regexp
	 *            The regular expression to search,
	 * @param replacement
	 *            The replacement string,
	 * @param countperpage
	 *            The maximum number of match per page.
	 */
	public void addReplace(String regexp, String replacement, int countperpage) {
		searchEntries.add(new ReplaceEntry(regexp, replacement, countperpage));
	}

	/**
	 * Add a new entry to replace all substrings of text elements that are the
	 * first per page to match the given regular expression with the given
	 * replacement, in a given range of pages of the PDF document.
	 * 
	 * @param regexp
	 *            The regular expression to search,
	 * @param replacement
	 *            The replacement string,
	 * @param countperpage
	 *            The maximum number of match per page,
	 * @param startpage
	 *            The page number to start from,
	 * @param endpage
	 *            The page number to end.
	 */
	public void addReplace(String regexp, String replacement, int countperpage,
			int startpage, int endpage) {
		searchEntries.add(new ReplaceEntry(regexp, replacement, countperpage,
				startpage, endpage));
	}

	/**
	 * Perform the replacements on the in-memory PDF document.
	 * 
	 * @param doc
	 *            The in-memory representation of the PDF document.
	 * @throws IOException
	 *             If an error occurred while performing the replacements.
	 */
	public void processDocument(PDDocument doc) throws IOException {
		Map<String, PDFont> docFontCache = new HashMap<String, PDFont>();
		@SuppressWarnings("unchecked")
		List<PDPage> pages = doc.getDocumentCatalog().getAllPages();
		for (int i = 0; i < pages.size(); i++) {
			curPagesearchEntries = new ArrayList<ReplaceEntry>();
			Iterator<ReplaceEntry> it = searchEntries.iterator();
			while (it.hasNext()) {
				ReplaceEntry search = it.next();
				if ((search.startpage <= i + 1) && (i + 1 <= search.endpage)) {
					for (int j = 0; j < search.countperpage; j++)
						curPagesearchEntries.add(search);
				}
			}
			if (curPagesearchEntries.size() > 0)
				processPage(doc, pages.get(i), docFontCache);
		}
	}

	private void processPage(PDDocument doc, PDPage page,
			Map<String, PDFont> docFontCache) throws IOException {
		@SuppressWarnings("unchecked")
		Map<String, PDFont> curPageFonts = page.findResources().getFonts(docFontCache);
		PDFont curFont = null;
		String curFontName = null;
		long curFontSize = 12;
		PDStream contents = page.getContents();
		if(contents != null) {
			PDFStreamParser parser = new PDFStreamParser(contents.getStream());
			try {
				parser.parse();
				List<Object> tokens = parser.getTokens();
				List<COSBase> arguments = new ArrayList<COSBase>();
				for (int i = 0; i < tokens.size(); i++) {
					Object curToken = tokens.get(i);
					if (curToken instanceof COSObject) {
						arguments.add(((COSObject) curToken).getObject());
					} else if (curToken instanceof PDFOperator) {
						boolean requiresFontChange = false;
						String operation = ((PDFOperator) curToken).getOperation();
						if ("Tf".equals(operation)) {
							// Tf takes two arguments: the font name and size
							curFontName = ((COSName) arguments.get(0)).getName();
							curFontSize = ((COSNumber) arguments.get(1)).longValue();
							curFont = curPageFonts.get(curFontName);
						} else if ("Tj".equals(operation)) {
							// Tj takes one argument: the string to display
							requiresFontChange = processString((COSString) arguments.get(0), curFont);
						} else if ("TJ".equals(operation)) {
							// TJ takes one argument: an array of string and integers (to adjust text position)
							requiresFontChange = processStringArray((COSArray) arguments.get(0), curFont);
						}
						if(requiresFontChange) {
							String newFontMapping = null;
							for(Entry<String,PDFont> fontEntry : curPageFonts.entrySet()) {
								if(subFont.equals(fontEntry.getValue()))
									newFontMapping = fontEntry.getKey();
							}
							if(newFontMapping == null) {
								newFontMapping = MapUtil.getNextUniqueKey(curPageFonts, "F");
								curPageFonts.put(newFontMapping, subFont);
							}
							PDFOperator fontOp = PDFOperator.getOperator("Tf");
							COSInteger fontSi = COSInteger.get(curFontSize);
							tokens.add(i+1, fontOp);
							tokens.add(i+1, fontSi);
							tokens.add(i+1, COSName.getPDFName(curFontName));
							tokens.add(i-1, fontOp);
							tokens.add(i-1, COSInteger.get(curFontSize));
							tokens.add(i-1, COSName.getPDFName(newFontMapping));
							i += 6;
						}
						arguments = new ArrayList<COSBase>();
					} else {
						arguments.add((COSBase) curToken);
					}
					if(curPagesearchEntries.size() == 0)
						break;
				}
				// now that the tokens are updated we will replace the page content
				// stream.
				PDStream updatedStream = new PDStream(doc);
				updatedStream.addCompression();
				OutputStream out = updatedStream.createOutputStream();
				ContentStreamWriter tokenWriter = new ContentStreamWriter(out);
				tokenWriter.writeTokens(tokens);
				page.setContents(updatedStream);
			} finally {
				parser.close();
			}
		}
	}

	private boolean processString(COSString cosString, PDFont curFont) throws IOException {
		String text = curFont == null ? cosString.getString() : decodeText(cosString.getBytes(),curFont);
		boolean requiresFontChange = false;
		for(int i=0; i<curPagesearchEntries.size(); i++) {
			ReplaceEntry search = curPagesearchEntries.get(i);
			if (search.matches(text)) {
				String repText = search.replace(text);
				if(repText.length() > 0 && curFont != null) {
					Encoding encoding = curFont.getFontEncoding();
					if(encoding == null) {
						encoding = subFont.getFontEncoding();
						requiresFontChange = true;
						log.warn("Changing the font on \"" + repText + "\" to substitute.");
					}
					repText = encodeText(repText, encoding);
				}
				cosString.reset();
				cosString.append(new COSString(repText).getBytes());
				curPagesearchEntries.remove(i);
				i--;
			}
		}
		return requiresFontChange;
	}

	private boolean processStringArray(COSArray cosArray, PDFont curFont) throws IOException {
		int size = cosArray.size();
		StringBuilder strBuilder = new StringBuilder();
		for (int i = 0; i < size; i++) {
			COSBase obj = cosArray.getObject(i);
			if (obj instanceof COSString) {
				String str = curFont == null ? ((COSString)obj).getString() : decodeText(((COSString)obj).getBytes(),curFont);
				strBuilder.append(str);
			}
		}
		String text = strBuilder.toString();
		boolean requiresFontChange = false;
		for(int i=0; i<curPagesearchEntries.size(); i++) {
			ReplaceEntry search = curPagesearchEntries.get(i);
			if (search.matches(text)) {
				String repText = search.replace(text);
				if(repText.length() > 0 && curFont != null) {
					Encoding encoding = curFont.getFontEncoding();
					if(encoding == null) {
						encoding = subFont.getFontEncoding();
						requiresFontChange = true;
						log.warn("Changing the font on \"" + repText + "\" to substitute.");
					}
					repText = encodeText(repText, encoding);
				}
				cosArray.clear();
				cosArray.add(new COSString(repText));
				curPagesearchEntries.remove(i);
				i--;
			}
		}
		return requiresFontChange;
	}

	private String decodeText(byte[] bytes, PDFont curFont) throws IOException {
		StringBuilder charStr = new StringBuilder();
		int codeLength = 1;
		for (int i = 0; i < bytes.length; i += codeLength) { // Decode the value to a Unicode character
			codeLength = 1;
			String c = curFont.encode(bytes, i, codeLength);
			if (c == null && i + 1 < bytes.length) { // maybe a multibyte encoding
				codeLength++;
				c = curFont.encode(bytes, i, codeLength);
			}
			charStr.append(c);
		}
		return charStr.toString();
	}
	
	private String encodeText(String string, Encoding encoding) throws IOException {
		Map<String, Integer> nameToCode = encoding.getNameToCodeMap();
		StringBuilder codeStr = new StringBuilder();
		for (int i = 0; i < string.length(); i++) {
			String c = string.substring(i, i+1);
			Integer code = nameToCode.get(c);
			if(code != null)
				codeStr.append((char) code.intValue());
			else // No code for the character, default to unicode
				codeStr.append(c);
		}
		return codeStr.toString();
	}

}
