

package main
import (
	"fmt"
	"strconv"

	"github.com/hyperledger/fabric/core/chaincode/shim"
	pb "github.com/hyperledger/fabric/protos/peer"
)

// SimpleChaincode example simple Chaincode implementation
type SimpleChaincode struct {
}

// Init initializes the chaincode state
func (t *SimpleChaincode) Init(stub shim.ChaincodeStubInterface) pb.Response {
	fmt.Println("########### example_cc Init ###########")
	_, args := stub.GetFunctionAndParameters()
	var A, B string    // Entities
	var Aval, Bval int // Asset holdings

	// Initialize the chaincode
	A = args[0]
	Aval, _ = strconv.Atoi(args[1])

	B = args[2]
	Bval, _ = strconv.Atoi(args[3])

	fmt.Printf("Aval = %d, Bval = %d\n", Aval, Bval)

	// Write the state to the ledger
	stub.PutState(A, []byte(strconv.Itoa(Aval)))
	stub.PutState(B, []byte(strconv.Itoa(Bval)))


    transientMap, _ := stub.GetTransient();
    transientData, _ := transientMap["result"];
    return shim.Success(transientData)



}

// Invoke makes payment of X units from A to B
func (t *SimpleChaincode) Invoke(stub shim.ChaincodeStubInterface) pb.Response {
	fmt.Println("########### example_cc Invoke ###########")
	_, args := stub.GetFunctionAndParameters()

	if args[0] == "query" {
		// queries an entity state
		return t.query(stub, args)
	}

    return shim.Error("Unknown action")
}


func (t *SimpleChaincode) query(stub shim.ChaincodeStubInterface, args []string) pb.Response {

	var A string // Entities

	A = args[1]

	Avalbytes, _ := stub.GetState(A)

	return shim.Success(Avalbytes)
}

func main() {
	shim.Start(new(SimpleChaincode))
}
