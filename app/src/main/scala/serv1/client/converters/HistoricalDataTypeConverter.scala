package serv1.client.converters

import serv1.db.types.HistoricalDataType.HistoricalDataType

object HistoricalDataTypeConverter {
  def getHistoricalDataType(historicalDataType: HistoricalDataType): String = {
    historicalDataType.toString
  }
}
