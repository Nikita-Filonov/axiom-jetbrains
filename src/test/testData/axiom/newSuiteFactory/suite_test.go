package accounts

import (
	"testing"

	"github.com/Nikita-Filonov/axiom"
)

func TestAccountServiceSuite(t *testing.T) {
	suite := axiom.NewSuiteFactory(t, func() *accountSuite {
		return &accountSuite{}
	})
	suite.Run()
}

type accountSuite struct {
	axiom.Suite
}

func (s *accountSuite) TestGetAccount() {}
