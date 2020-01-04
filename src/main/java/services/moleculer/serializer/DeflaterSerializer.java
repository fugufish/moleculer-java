/**
 * THIS SOFTWARE IS LICENSED UNDER MIT LICENSE.<br>
 * <br>
 * Copyright 2019 Andras Berkes [andras.berkes@programmer.net]<br>
 * Based on Moleculer Framework for NodeJS [https://moleculer.services].
 * <br><br>
 * Permission is hereby granted, free of charge, to any person obtaining
 * a copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to
 * the following conditions:<br>
 * <br>
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.<br>
 * <br>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
 * LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
 * OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package services.moleculer.serializer;

import static services.moleculer.util.CommonUtils.compress;
import static services.moleculer.util.CommonUtils.decompress;
import static services.moleculer.util.CommonUtils.formatNamoSec;

import java.util.zip.Deflater;

import io.datatree.Tree;
import services.moleculer.service.Name;

/**
 * Message compressor / decompressor. Sample of usage:<br>
 * 
 * <pre>
 * Transporter trans = new NatsTransporter("localhost");
 * trans.setSerializer(new DeflaterSerializer());
 * ServiceBroker broker = ServiceBroker.builder()
 *                                     .nodeID("node1")
 *                                     .transporter(trans)
 *                                     .build();
 * </pre>
 * 
 * Chaining Serializers (serialize then compress then encrypt packets):
 * 
 * <pre>
 * Transporter trans = new NatsTransporter("localhost");
 * MsgPackSerializer msgPack = new MsgPackSerializer();
 * DeflaterSerializer deflater = new DeflaterSerializer(msgPack);
 * BlockCipherSerializer cipher = new BlockCipherSerializer(deflater);
 * trans.setSerializer(cipher);
 * </pre>
 */
@Name("Deflater Serializer")
public class DeflaterSerializer extends ChainedSerializer {

	// --- PROPERTIES ---

	/**
	 * Compress key and/or value above this size (BYTES), 0 = disable
	 * compression
	 */
	protected int compressAbove = 1024;

	/**
	 * Compression level (best speed = 1, best compression = 9)
	 */
	protected int compressionLevel = Deflater.BEST_SPEED;

	// --- CONSTRUCTORS ---

	/**
	 * Creates makes a JSON-based Serializer that compresses content above a
	 * specified size (see the "compressAbove" parameter).
	 */
	protected DeflaterSerializer() {
		super(new JsonSerializer());
	}

	/**
	 * Creates a custom Serializer that compresses content above a specified
	 * size (see the "compressAbove" parameter).
	 * 
	 * @param parent
	 *            parent Serializer (eg. a JsonSerializer)
	 */
	public DeflaterSerializer(Serializer parent) {
		super(parent);
	}

	// --- SERIALIZE AND COMPRESS TREE TO BYTE ARRAY ---

	public byte[] write(Tree value) throws Exception {

		// Serialize content
		byte[] bytes = parent.write(value);

		// Compress content
		boolean compressed;
		if (compressAbove > 0 && bytes.length > compressAbove) {
			if (debug) {
				long start = System.nanoTime();
				byte[] compressedBytes = compress(bytes, compressionLevel);
				long duration = System.nanoTime() - start;
				logger.info("Packet compressed in " + formatNamoSec(duration) + " (from " + bytes.length + " bytes to "
						+ compressedBytes.length + " bytes).");
				bytes = compressedBytes;
			} else {
				bytes = compress(bytes, compressionLevel);
			}
			compressed = true;
		} else {
			compressed = false;
		}
		byte[] copy = new byte[bytes.length + 1];
		System.arraycopy(bytes, 0, copy, 1, bytes.length);
		if (compressed) {

			// Compressed -> first byte = 1
			copy[0] = (byte) 1;
		}
		return copy;
	}

	// --- DECOMPRESS AND DESERIALIZE BYTE ARRAY TO TREE ---

	public Tree read(byte[] source) throws Exception {

		// Decompress content
		byte[] copy = new byte[source.length - 1];
		System.arraycopy(source, 1, copy, 0, source.length - 1);
		if (source[0] == 1) {

			// First byte == 1 -> compressed
			if (debug) {
				long start = System.nanoTime();
				byte[] decompressed = decompress(copy);
				long duration = System.nanoTime() - start;
				logger.info("Packet extracted in " + formatNamoSec(duration) + " (from " + copy.length + " bytes to "
						+ decompressed.length + " bytes).");
				copy = decompressed;
			} else {
				copy = decompress(copy);
			}
		}

		// Deserialize content
		return parent.read(copy);
	}

	// --- GETTERS / SETTERS ---

	public int getCompressionLevel() {
		return compressionLevel;
	}

	public void setCompressionLevel(int compressionLevel) {
		this.compressionLevel = compressionLevel;
	}

	public int getCompressAbove() {
		return compressAbove;
	}

	public void setCompressAbove(int compressAbove) {
		this.compressAbove = compressAbove;
	}

}