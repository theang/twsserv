package serv1.client.operations

case class ClientOperation[AccType, Callback](contP: Callback, dataP: AccType) {
  var cont: Callback = contP
  var data: AccType = dataP
}
