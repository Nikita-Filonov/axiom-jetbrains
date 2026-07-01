package cards

import (
	"testing"

	"github.com/Nikita-Filonov/axiom"
)

func TestCardServiceSuite(t *testing.T) {
	suite := axiom.NewSuite(t, new(serviceSuite))
	suite.Run()
}

type serviceSuite struct {
	axiom.Suite
}

func (s *serviceSuite) TestGetCard() {
	// runnable
}

func (s *serviceSuite) TestListCards() {
	// runnable
}

// Helper – not a test.
func (s *serviceSuite) loadFixture(name string) string {
	return name
}
