package serv1.module

import serv1.db.repo.intf.TickerDataRepoIntf

trait ModuleIntf {
  def init(tickerDataRepoIntf: TickerDataRepoIntf): Unit

  def calc(): Unit
}
