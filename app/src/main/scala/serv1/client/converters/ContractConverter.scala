package serv1.client.converters

import com.ib.client.Contract
import com.ib.client.Types.SecType

object ContractConverter {
  def getContract(ticker:String, exchange: String, typ: String): Contract = {
    var contract = new Contract()
    contract.exchange(exchange)
    var secType = SecType.get(typ)
    contract.secType(secType)
    contract.symbol(ticker)
    contract.currency("USD")
    contract
  }
}
