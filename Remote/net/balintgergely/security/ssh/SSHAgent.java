package net.balintgergely.security.ssh;

import java.io.Closeable;
import java.io.IOException;
import java.nio.channels.ByteChannel;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

public class SSHAgent implements Closeable{
	// Connect to SSH-agent via pipe: \\.\pipe\openssh-ssh-agent
	// Interface using ssh agent protocol: https://datatracker.ietf.org/doc/html/draft-miller-ssh-agent-13
	private static ByteChannel openChannel() throws IOException{
		return FileChannel.open(
			Path.of("\\\\.\\pipe\\openssh-ssh-agent"),
			StandardOpenOption.WRITE, StandardOpenOption.READ);
	}
	private ByteChannel channel;
	private WireWriter writer;
	private WireReader reader;
	public SSHAgent() throws IOException{
		channel = openChannel();
		writer = new WireWriter(Channels.newOutputStream(channel));
		reader = new WireReader(Channels.newInputStream(channel));
	}
	public synchronized byte[] sign(byte[] data,byte[] key) throws IOException{
		try(WireWriter blob = writer.writeBlob()){
			blob.writeByte((byte)13);// 13 = SSH_AGENTC_SIGN_REQUEST
			blob.writeByteArray(key);
			blob.writeByteArray(data);
			blob.writeInt(0); // flags	
		}
		writer.flush();
		WireReader blob = reader.readBlob();
		byte response = blob.readByte();
		if(response != 14){
			throw new IOException("SSH agent refused the operation");
		}
		blob.skip(4); // Entering another blob.
		blob.skip(blob.readInt()); // Skip algorithm name.
		return blob.readByteArray();
	}
	@Override
	public void close() throws IOException {
		channel.close();
	}
}
