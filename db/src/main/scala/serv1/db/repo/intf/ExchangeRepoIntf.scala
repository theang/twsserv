package serv1.db.repo.intf

trait ExchangeRepoIntf {
  def getExchangeId(name: String): Int
}
