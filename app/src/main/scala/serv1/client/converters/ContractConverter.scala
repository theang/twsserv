package serv1.client.converters

import com.ib.client.Contract
import com.ib.client.Types.SecType
import serv1.model.ticker.TickerType

object ContractConverter {
  def getContract(tickerType: TickerType): Contract = {
    var contract = new Contract()
    contract.exchange(tickerType.exchange)
    var secType = SecType.get(tickerType.typ)
    contract.secType(secType)
    contract.symbol(tickerType.name)
    contract.currency("USD")
    tickerType.lastTradeDateOrContractMonth.foreach(contract.lastTradeDateOrContractMonth(_))
    tickerType.primaryExchange.foreach(contract.primaryExch(_))
    contract
  }
}
