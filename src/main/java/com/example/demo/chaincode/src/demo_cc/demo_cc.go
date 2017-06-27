package main

import (
	"strconv"
	"encoding/json"
	"github.com/hyperledger/fabric/core/chaincode/shim"
	pb "github.com/hyperledger/fabric/protos/peer"
)

type SimpleChaincode struct {
	//中奖号码
	LuckyNumber string
	//名称
	FullName string
	//资产
	Asset int

}

func (t *SimpleChaincode) Init(stub shim.ChaincodeStubInterface) pb.Response {
	//
	var center, custom1, custom2 SimpleChaincode
	//
	center.LuckyNumber = "0"
	center.FullName = "彩票中心"
	center.Asset = 200
	//
	custom1.LuckyNumber = "0"
	custom1.FullName = "用户1"
	custom1.Asset = 20
	//
	custom2.LuckyNumber = "0"
	custom2.FullName = "用户2"
	custom2.Asset = 50

	//将state写入账本ledger中
	byteData, _ := json.Marshal(center)//彩票中心
	stub.PutState("center", byteData)
	byteData, _ = json.Marshal(custom1)//用户1
	stub.PutState("custom1", byteData)
	byteData, _ = json.Marshal(custom2)//用户2
	stub.PutState("custom2", byteData)
	stub.PutState("isEventStarted", []byte(strconv.Itoa(0)))//活动是否启动，0代表未启动，1代表启动

	//
	transientMap, _ := stub.GetTransient();
	transientData, _ := transientMap["result"];
	return shim.Success(transientData)
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
	//购买幸运号码
	if args[0] == "buyLuckyNumber" {
		return t.buyLuckyNumber(stub, args)
	}
	//报出幸运号码
	if args[0] =="inputLuckyNumber" {
		return t.inputLuckyNumber(stub, args)
	}
	return shim.Error("*** 未知命令 *** " + args[0])
}

func (t *SimpleChaincode) inputLuckyNumber(stub shim.ChaincodeStubInterface, args []string) pb.Response {
	//入参为：0方法名，1幸运数字
	centerBytes, _ := stub.GetState("center")
	var bytes []byte
	var center, custom1 SimpleChaincode
	json.Unmarshal(centerBytes, &center)
	center.LuckyNumber = args[1]

	custom1Bytes, _ := stub.GetState("custom1")
	json.Unmarshal(custom1Bytes, &custom1)
	if(custom1.LuckyNumber == center.LuckyNumber) {
		center.Asset = center.Asset - 20

		custom1.Asset = custom1.Asset + 20
		bytes, _ = json.Marshal(custom1)
		stub.PutState("custom1", bytes)
	}
	bytes, _ = json.Marshal(center)
	stub.PutState("center", bytes)

	return shim.Success(nil)
}

/**
购买幸运号码
 */
func (t *SimpleChaincode)  buyLuckyNumber(stub shim.ChaincodeStubInterface, args []string) pb.Response {
	//入参为：0方法名 1用户名 2号码
	var bytes []byte

	//用户购买，减钱，添加幸运号码
	customBytes, _ := stub.GetState(args[1])
	var custom SimpleChaincode
	json.Unmarshal(customBytes, &custom)
	custom.LuckyNumber = args[2]
	custom.Asset = custom.Asset - 10
	bytes, _ = json.Marshal(custom)
	stub.PutState(args[1], bytes)

	//彩票中心，加钱，
	centerBytes, _ := stub.GetState("center")
	var center SimpleChaincode
	json.Unmarshal(centerBytes, &center)
	center.Asset = center.Asset + 10
	bytes, _ = json.Marshal(center)
	stub.PutState("center", bytes)

	return shim.Success(nil)
}

/**
更新活动状态
 */
func (t *SimpleChaincode) updateIsEventStarted(stub shim.ChaincodeStubInterface, args []string) pb.Response {
	_ = stub.PutState("isEventStarted", []byte(args[1]))
	return shim.Success(nil)
}
/**
返回当前活动状态
 */
func (t *SimpleChaincode) isEventStarted(stub shim.ChaincodeStubInterface, args []string) pb.Response {
	result, _ := stub.GetState("isEventStarted")

	return shim.Success(result)
}
/**
查询struct的信息
 */
func (t *SimpleChaincode) query(stub shim.ChaincodeStubInterface, args []string) pb.Response {
	result, _ := stub.GetState(args[1])
	return shim.Success(result)
}

func main() {
	shim.Start(new(SimpleChaincode))
}
