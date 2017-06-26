package main

import (

	"strconv"
	 "bytes"
	"github.com/hyperledger/fabric/core/chaincode/shim"
	pb "github.com/hyperledger/fabric/protos/peer"
)

type SimpleChaincode struct {
	//中奖号码
	luckyNumber string
	//名称
	fullName string
	//资产
	asset int

}

// Init initializes the chaincode state
func (t *SimpleChaincode) Init(stub shim.ChaincodeStubInterface) pb.Response {
	//
	var center, custom1, custom2 SimpleChaincode
	var buf bytes.Buffer
	//
	center.luckyNumber = "0"
	center.fullName = "彩票中心"
	center.asset = 100
	//
	custom1.luckyNumber = "0"
	custom1.fullName = "用户1"
	custom1.asset = 10
	//
	custom2.luckyNumber = "0"
	custom2.fullName = "用户2"
	custom2.asset = 10


	//将state写入账本ledger中
	_ := binary.Write(&buf, binary.LittleEndian, center)
	stub.PutState("center", buf)//彩票中心
	_ := binary.Write(&buf, binary.LittleEndian, custom1)
	stub.PutState("custom1", custom1)//用户1
	_ := binary.Write(&buf, binary.LittleEndian, custom2)
	stub.PutState("custom2", custom2)//用户2
	stub.PutState("isEventStarted", []byte(strconv.Itoa(0)))//活动是否启动，0代表未启动，1代表启动

	//
	transientMap, _ := stub.GetTransient();
	transientData, _ := transientMap["result"];
	return shim.Success(transientData)
}

func (t *SimpleChaincode) query(stub shim.ChaincodeStubInterface) pb.Response {
	_, args := stub.GetFunctionAndParameters()
	var key string
	key = args[1]
	result, _ := stub.GetState(key)
	return shim.Success(result)
}

func (t *SimpleChaincode) Invoke(stub shim.ChaincodeStubInterface) pb.Response {
	_, args := stub.GetFunctionAndParameters()
	//查询活动是否开启
	if args[0] == "isEventStarted" {
		return t.isEventStarted(stub, args)
	}
	//更新活动状态，0为关闭，1为开启
	if args[0] == "updateIsEventStarted" {
		return t.updateIsEventStarted(stub, args)
	}
	//查询彩票中心，和用户信息
	if args[0] == "query" {
		return t.query(stub, args)
	}
	return shim.Error("*** Unknown action *** " + args[0])
}

func (t *SimpleChaincode) updateIsEventStarted(stub shim.ChaincodeStubInterface, args []string) pb.Response {
	_ = stub.PutState("isEventStarted", []byte(args[1]))
	return shim.Success(nil)
}

func (t *SimpleChaincode) isEventStarted(stub shim.ChaincodeStubInterface, args []string) pb.Response {
	result, _ := stub.GetState("isEventStarted")
	return shim.Success(result)
}


func main() {
	shim.Start(new(SimpleChaincode))
}
