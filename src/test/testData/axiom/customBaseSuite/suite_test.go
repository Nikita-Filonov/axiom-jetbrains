package custom

import (
	"testing"

	"github.com/Nikita-Filonov/axiom"
	"gitlab.diftech.org/processing/caa/test-tools/testsuite"
)

// Framework-agnostic: users may embed a company-specific base suite.
// The detector must still recognise `mySuite` because it is passed to
// axiom.NewSuite in the registering function.

func TestMyServiceSuite(t *testing.T) {
	axiom.NewSuite(t, new(mySuite)).Run()
}

type mySuite struct {
	testsuite.BaseSuite
}

func (s *mySuite) TestSomething() {}
