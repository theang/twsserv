package serv1.backend.serialize

import serv1.db.schema.TickerData

import java.nio.{ByteBuffer, ByteOrder}

object SerializeTOHLCV {
  def serialize(data: Seq[TickerData]): Seq[Byte] = {
    data.flatMap(f => {
      val size = 8 * 6
      val bb = ByteBuffer.allocate(size)
      bb.order(ByteOrder.LITTLE_ENDIAN)
      bb.putLong(f.time)
      bb.putDouble(f.open)
      bb.putDouble(f.high)
      bb.putDouble(f.low)
      bb.putDouble(f.close)
      bb.putDouble(f.vol)
      bb.flip()
      val bbRes = new Array[Byte](size)
      bb.get(bbRes)
      bbRes.toSeq
    })
  }
}
