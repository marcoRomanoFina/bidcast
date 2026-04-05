import { useEffect, useState, useRef } from "react"
import { Button } from "@/components/ui/button"
import { Card, CardContent } from "@/components/ui/card"
import { Badge } from "@/components/ui/badge"
import { Separator } from "@/components/ui/separator"
import { 
  Monitor, 
  BarChart3, 
  ArrowRight,
  Zap,
  Globe,
  ShieldCheck,
  Tv,
  PlayCircle,
  Wallet
} from "lucide-react"
import { Link } from "react-router-dom"

// Scroll reveal hook
function useReveal() {
  const ref = useRef<HTMLDivElement>(null);
  const [isVisible, setIsVisible] = useState(false);

  useEffect(() => {
    const observer = new IntersectionObserver(
      ([entry]) => {
        if (entry.isIntersecting) {
          setIsVisible(true);
          observer.unobserve(entry.target);
        }
      },
      { threshold: 0.1 }
    );
    if (ref.current) observer.observe(ref.current);
    return () => observer.disconnect();
  }, []);

  return { ref, isVisible };
}

function LandingPage() {
  const [scrolled, setScrolled] = useState(false);
  const introReveal = useReveal();
  const solutionReveal = useReveal();
  const flowReveal = useReveal();
  const carouselReveal = useReveal();
  const analyticsReveal = useReveal();

  useEffect(() => {
    const handleScroll = () => setScrolled(window.scrollY > 50);
    window.addEventListener("scroll", handleScroll);
    return () => window.removeEventListener("scroll", handleScroll);
  }, []);

  return (
    <div className="min-h-screen bg-background text-foreground font-sans selection:bg-primary/30 selection:text-primary overflow-x-hidden">
      {/* Navbar - Dynamic visibility and styling */}
      <nav className={`fixed top-0 z-50 w-full transition-all duration-500 border-b ${scrolled ? 'h-20 bg-background/80 backdrop-blur-md border-white/5' : 'h-24 bg-transparent border-transparent'}`}>
        <div className="container mx-auto flex h-full items-center justify-between px-4 sm:px-6 lg:px-8">
          <Link to="/" className={`flex items-center gap-4 group transition-all duration-500 ${scrolled ? 'opacity-100 translate-x-0' : 'opacity-0 -translate-x-4 pointer-events-none'}`}>
            <div className="flex h-12 w-12 items-center justify-center rounded-2xl bg-primary text-black shadow-[0_0_30px_rgba(236,212,68,0.3)] group-hover:scale-110 transition-transform duration-500">
              <Tv size={28} strokeWidth={2.5} />
            </div>
            <span className="text-3xl font-black tracking-tighter text-foreground uppercase tracking-widest text-white">BIDCAST</span>
          </Link>
          
          <div className="flex items-center gap-4">
            <Link to="/login">
              <Button variant="ghost" size="lg" className={`font-black uppercase tracking-widest text-xs hidden sm:flex transition-colors duration-500 ${scrolled ? 'text-foreground hover:text-primary' : 'text-black hover:bg-black/5'}`}>Ingresar</Button>
            </Link>
            <Link to="/login">
              <Button size="lg" className={`bg-primary text-black hover:bg-primary/90 font-black px-8 h-14 rounded-2xl shadow-[0_10px_20px_rgba(236,212,68,0.2)] uppercase transition-all hover:-translate-y-1`}>COMENZAR</Button>
            </Link>
          </div>
        </div>
      </nav>

      <main>
        {/* Yellow Banner Section */}
        <section className="bg-primary pt-48 pb-32 md:pt-64 md:pb-48 relative overflow-hidden">
          {/* Floating Background Devices */}
          <div className="absolute inset-0 pointer-events-none overflow-hidden opacity-[0.15]">
            <Monitor className="absolute top-[10%] left-[15%] text-black w-8 h-8 animate-drift-1" strokeWidth={3} />
            <Tv className="absolute top-[25%] left-[5%] text-black w-6 h-6 animate-drift-2" strokeWidth={3} />
            <Monitor className="absolute bottom-[20%] left-[10%] text-black w-10 h-10 animate-drift-3" strokeWidth={3} />
            <Tv className="absolute top-[15%] right-[15%] text-black w-8 h-8 animate-drift-2" strokeWidth={3} />
            <Monitor className="absolute top-[40%] right-[5%] text-black w-6 h-6 animate-drift-1" strokeWidth={3} />
            <Tv className="absolute bottom-[15%] right-[10%] text-black w-12 h-12 animate-drift-3" strokeWidth={3} />
          </div>

          <div className="container mx-auto px-4 sm:px-6 lg:px-8 relative z-10 text-black">
            <div className="flex flex-col items-center text-center">
              <div className="mb-12 flex h-32 w-32 items-center justify-center rounded-[2.5rem] bg-background text-primary shadow-2xl animate-in fade-in zoom-in duration-1000">
                <Tv size={64} strokeWidth={2.5} />
              </div>
              <h1 className="text-7xl md:text-9xl lg:text-[10rem] font-black tracking-tighter leading-none mb-12 uppercase">
                BIDCAST
              </h1>
              <div className="max-w-3xl bg-background text-foreground p-10 md:p-16 rounded-[3rem] shadow-2xl border border-white/5 animate-in slide-in-from-bottom-10 duration-1000 delay-200 hover:scale-[1.02] transition-transform">
                <p className="text-3xl md:text-4xl font-black italic tracking-tight leading-tight uppercase text-white">
                  Tus anuncios en cualquier pantalla. Literalmente.
                </p>
              </div>

              {/* Technical Pillars */}
              <div className="mt-24 flex flex-wrap justify-center gap-16 md:gap-32 animate-in fade-in slide-in-from-bottom-4 duration-1000 delay-500">
                <div className="flex flex-col items-center group cursor-default">
                  <div className="h-16 w-16 rounded-[1.5rem] bg-black text-primary flex items-center justify-center mb-4 shadow-2xl group-hover:scale-110 group-hover:rotate-12 transition-all duration-500">
                    <Zap size={32} strokeWidth={2.5} />
                  </div>
                  <span className="text-xs font-black uppercase tracking-[0.2em]">RTB Engine</span>
                  <span className="text-[10px] font-bold text-black/40 uppercase">Subastas en ms</span>
                </div>
                
                <div className="flex flex-col items-center group cursor-default">
                  <div className="h-16 w-16 rounded-[1.5rem] bg-black text-primary flex items-center justify-center mb-4 shadow-2xl group-hover:scale-110 group-hover:-rotate-12 transition-all duration-500">
                    <PlayCircle size={32} strokeWidth={2.5} />
                  </div>
                  <span className="text-xs font-black uppercase tracking-[0.2em]">Ad Sessions</span>
                  <span className="text-[10px] font-bold text-black/40 uppercase text-center">Pantallas que monetizan</span>
                </div>

                <div className="flex flex-col items-center group cursor-default">
                  <div className="h-16 w-16 rounded-[1.5rem] bg-black text-primary flex items-center justify-center mb-4 shadow-2xl group-hover:scale-110 group-hover:rotate-6 transition-all duration-500">
                    <Globe size={32} strokeWidth={2.5} />
                  </div>
                  <span className="text-xs font-black uppercase tracking-[0.2em]">Device Network</span>
                  <span className="text-[10px] font-bold text-black/40 uppercase text-center">Eleguí entre miles de dispositivos</span>
                </div>
              </div>
            </div>
          </div>
        </section>

        {/* Introductory Value Proposition Section */}
        <section ref={introReveal.ref} className={`py-40 bg-[#121214] border-b border-white/5 relative overflow-hidden transition-all duration-1000 ${introReveal.isVisible ? 'opacity-100 translate-y-0' : 'opacity-0 translate-y-20'}`}>
          <div className="absolute top-0 left-1/2 -translate-x-1/2 w-full h-px bg-gradient-to-r from-transparent via-primary/30 to-transparent" />
          
          <div className="container mx-auto px-4 sm:px-6 lg:px-8 relative z-10 text-white">
            <div className="max-w-5xl mx-auto text-center">
              <h2 className="text-6xl md:text-9xl font-black tracking-tighter leading-[0.85] mb-16 uppercase">
                REDEFINIMOS EL <br />
                <span className="text-primary italic">OUT-OF-HOME.</span>
              </h2>
              <p className="text-2xl md:text-4xl font-bold text-foreground/40 leading-tight uppercase tracking-tight mb-24 max-w-4xl mx-auto italic">
                BidCast es el puente de alta velocidad entre el marketing digital y el mundo físico.<br ></br> Publicidad a velocidad de software. Sin fricción, sin esperas.
              </p>
              
              <div className="grid grid-cols-1 md:grid-cols-3 gap-16 text-left border-t border-white/5 pt-20">
                {[
                  { title: "Cero Intermediarios", desc: "Conexión directa de anunciante a pantalla. Sin comisiones ocultas ni esperas manuales." },
                  { title: "Control Total", desc: "Gestioná tus campañas de forma programática. Integración nativa con tu flujo de trabajo." },
                  { title: "Transparencia Real", desc: "Cada milisegundo de subasta es auditable. Sabés exactamente dónde y cuándo se muestra tu marca." }
                ].map((item, i) => (
                  <div key={i} className="space-y-6 group cursor-default">
                    <div className="h-1 w-12 bg-primary group-hover:w-24 transition-all duration-500" />
                    <h3 className="text-xl font-black text-foreground uppercase tracking-tighter group-hover:text-primary transition-colors">{item.title}</h3>
                    <p className="text-base text-foreground/40 font-medium leading-relaxed uppercase tracking-tight">{item.desc}</p>
                  </div>
                ))}
              </div>
            </div>
          </div>
          <div className="absolute -bottom-24 -right-24 w-96 h-96 bg-primary/5 rounded-full blur-[100px] pointer-events-none" />
        </section>

        {/* Sub-Hero / Solution */}
        <section id="solucion" ref={solutionReveal.ref} className={`relative py-40 overflow-hidden bg-background transition-all duration-1000 ${solutionReveal.isVisible ? 'opacity-100 translate-y-0' : 'opacity-0 translate-y-20'}`}>
          <div className="container mx-auto px-4 sm:px-6 lg:px-8 relative z-10 text-white text-center lg:text-left">
            <div className="grid grid-cols-1 lg:grid-cols-2 gap-24 items-center">
              <div className="flex flex-col items-center lg:items-start">
                <h2 className="text-6xl md:text-8xl font-black tracking-tighter leading-[0.9] mb-10 uppercase">
                  PUBLICIDAD <br />  SIN  <br /> 
                  <span className="text-primary italic">LIMITES.</span>
                </h2>
                <p className="text-2xl text-foreground/60 font-medium leading-relaxed mb-12 uppercase tracking-tight max-w-xl">
                  Tomá el control de las pantallas físicas en tiempo real. Subastá en milisegundos y poné tu marca donde realmente está la gente.
                </p>
                <div className="flex flex-col sm:flex-row gap-6">
                  <Link to="/login">
                    <Button size="lg" className="h-16 px-12 text-xl font-black bg-primary text-black hover:bg-primary/90 rounded-2xl shadow-xl transition-all hover:-translate-y-1">
                      CREAR CAMPAÑA <ArrowRight className="ml-2 h-6 w-6" />
                    </Button>
                  </Link>
                  <Button variant="outline" size="lg" className="h-16 px-12 text-xl font-black border-white/10 text-foreground hover:bg-white/5 rounded-2xl">
                    VER MAPA
                  </Button>
                </div>
              </div>
              
              {/* Live Device Session Demo */}
              <div className="relative group perspective-1000">
                <div className="absolute inset-0 bg-primary/10 blur-[120px] rounded-full group-hover:bg-primary/20 transition-all duration-1000" />
                <Card className="bg-white/5 backdrop-blur-xl border-white/10 shadow-2xl overflow-hidden relative z-10 transition-all duration-700 group-hover:rotate-y-6 group-hover:scale-[1.02]">
                  <div className="bg-background/50 px-8 py-4 flex justify-between items-center border-b border-white/5 text-white">
                    <div className="flex items-center gap-3">
                       <div className="h-3 w-3 rounded-full bg-primary animate-pulse" />
                       <span className="text-xs font-black uppercase tracking-[0.4em] text-primary">LIVE SESSION</span>
                    </div>
                    <span className="text-xs font-bold text-white/20 uppercase tracking-widest leading-none">ID: MAD-742</span>
                  </div>
                  <CardContent className="p-10 space-y-8">
                    <div className="flex items-center justify-between">
                      <div className="flex items-center gap-6">
                        <div className="h-16 w-16 rounded-[1.5rem] bg-primary text-black flex items-center justify-center shadow-[0_0_30px_rgba(236,212,68,0.4)]">
                          <Monitor size={32} strokeWidth={2.5} />
                        </div>
                        <div className="text-left">
                          <p className="text-lg font-black text-foreground leading-none mb-2 uppercase tracking-tighter">PANTALLA_C_04</p>
                          <p className="text-xs text-foreground/40 font-bold uppercase tracking-widest text-white/40">PLAZA MAYOR, MADRID</p>
                        </div>
                      </div>
                      <div className="text-right">
                        <p className="text-xs text-foreground/40 font-bold uppercase tracking-widest leading-none mb-2 text-white/40">Floor</p>
                        <p className="text-2xl font-black text-foreground leading-none font-mono text-white">$0.85</p>
                      </div>
                    </div>
                    
                    <Separator className="bg-white/10" />
                    
                    <div className="space-y-6">
                      <p className="text-[10px] font-black text-white/20 uppercase tracking-[0.3em] text-left">Incoming Bids</p>
                      <div className="space-y-4">
                        {[
                          { name: "CryptoExchange", price: "$2.45", opacity: "opacity-100", active: true },
                          { name: "Luxury Watches", price: "$2.10", opacity: "opacity-60", active: false },
                          { name: "Global Airlines", price: "$1.95", opacity: "opacity-30", active: false }
                        ].map((bid, i) => (
                          <div key={i} className={`flex items-center justify-between transition-all duration-500 ${bid.opacity}`}>
                            <div className="flex items-center gap-3 text-white text-left">
                              <div className={`h-2 w-2 rounded-full ${bid.active ? 'bg-primary' : 'bg-white/20'}`} />
                              <span className={`text-sm font-black uppercase ${bid.active ? 'text-primary' : 'text-foreground'}`}>{bid.name}</span>
                            </div>
                            <span className={`font-mono font-black text-lg ${bid.active ? 'text-primary' : 'text-foreground'}`}>{bid.price}</span>
                          </div>
                        ))}
                      </div>
                    </div>
                    
                    <div className="pt-4">
                      <div className="w-full bg-primary/10 border-2 border-primary/20 py-4 rounded-2xl flex items-center justify-center gap-3 transition-all duration-500 text-primary group-hover:bg-primary group-hover:text-black">
                        <Zap size={20} className="fill-current" />
                        <span className="text-sm font-black uppercase tracking-[0.2em] leading-none">WINNER: CRYPTOEXCHANGE</span>
                      </div>
                    </div>
                  </CardContent>
                </Card>
              </div>
            </div>
          </div>
        </section>

        {/* Consolidates Flow Section */}
        <section id="flujo" ref={flowReveal.ref} className={`py-40 bg-background border-y border-white/5 relative overflow-hidden transition-all duration-1000 ${flowReveal.isVisible ? 'opacity-100 translate-y-0' : 'opacity-0 translate-y-20'}`}>
          <div className="container mx-auto px-4 sm:px-6 lg:px-8 text-center relative z-10 mb-32">
            <Badge className="bg-primary text-black font-black mb-6 px-6 py-1.5 rounded-full text-xs uppercase tracking-widest">EL PROCESO</Badge>
            <h2 className="text-6xl md:text-9xl font-black tracking-tighter text-foreground uppercase leading-none tracking-widest text-white">MONETIZACIÓN <br />EN 4 PASOS.</h2>
          </div>

          <div className="container mx-auto px-4 sm:px-6 lg:px-8 text-white">
            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-10 relative">
              {[
                {
                  step: "01",
                  title: "REGISTRO",
                  desc: "Cargá tus dispositivos en la plataforma en segundos.",
                  visual: (
                    <div className="relative h-32 w-full flex items-center justify-center mb-10">
                      <div className="absolute inset-0 bg-primary/5 rounded-full blur-3xl" />
                      <div className="relative border-2 border-primary/20 p-6 rounded-[2rem] bg-background/50 group-hover:border-primary group-hover:scale-110 transition-all duration-500 shadow-2xl">
                        <Monitor size={48} className="text-primary" strokeWidth={1.5} />
                        <div className="absolute -top-3 -right-3 h-8 w-8 bg-primary text-black rounded-full flex items-center justify-center font-black text-lg">+</div>
                      </div>
                    </div>
                  )
                },
                {
                  step: "02",
                  title: "ACTIVACIÓN",
                  desc: "Iniciá la sesión y empezá a recibir solicitudes de puja.",
                  visual: (
                    <div className="relative h-32 w-full flex items-center justify-center mb-10">
                      <div className="relative bg-black border-2 border-primary/20 p-6 rounded-full shadow-[0_0_40px_rgba(236,212,68,0.2)] group-hover:scale-110 transition-all duration-500">
                        <Zap size={48} className="text-primary fill-primary/20" strokeWidth={1.5} />
                      </div>
                    </div>
                  )
                },
                {
                  step: "03",
                  title: "SUBASTA",
                  desc: "Los anunciantes compiten por aparecer en tus pantallas.",
                  visual: (
                    <div className="relative h-32 w-full flex items-center justify-center mb-10 text-white">
                      <div className="flex -space-x-6">
                        {[1, 2, 3].map(i => (
                          <div key={i} className={`h-16 w-16 rounded-2xl bg-background border-2 border-primary/20 flex items-center justify-center shadow-2xl transform transition-all duration-500 ${i === 2 ? 'scale-125 z-20 border-primary translate-y-[-10px]' : 'opacity-30 z-10 group-hover:opacity-50'}`}>
                            <PlayCircle size={28} className="text-primary" />
                          </div>
                        ))}
                      </div>
                    </div>
                  )
                },
                {
                  step: "04",
                  title: "PROFIT",
                  desc: "Recibí tus ganancias al finalizar cada sesión activa.",
                  visual: (
                    <div className="relative h-32 w-full flex items-center justify-center mb-10 text-primary text-white">
                      <Wallet size={64} strokeWidth={1.5} className="animate-bounce group-hover:scale-110 transition-transform text-primary" />
                      <div className="absolute bottom-2 flex gap-2">
                        {[1, 2, 3].map(i => (
                          <div key={i} className="h-2 w-6 bg-primary rounded-full animate-pulse" />
                        ))}
                      </div>
                    </div>
                  )
                }
              ].map((item, i) => (
                <Card key={i} className="bg-white/5 border-white/10 p-10 rounded-[3rem] hover:border-primary/50 transition-all duration-700 group relative z-10 backdrop-blur-md overflow-hidden h-full flex flex-col items-center hover:-translate-y-4">
                  <div className="w-full">
                    {item.visual}
                  </div>
                  <div className="flex flex-col items-center text-center">
                    <span className="text-xs font-black text-primary uppercase tracking-[0.4em] mb-4">{item.step}</span>
                    <h3 className="text-2xl font-black text-foreground mb-6 uppercase tracking-tight">{item.title}</h3>
                    <p className="text-base text-foreground/40 font-bold leading-relaxed uppercase tracking-tighter">
                      {item.desc}
                    </p>
                  </div>
                </Card>
              ))}
            </div>
          </div>
        </section>

        {/* Live Sessions Carousel Section */}
        <section ref={carouselReveal.ref} className={`py-40 bg-primary overflow-hidden relative transition-all duration-1000 ${carouselReveal.isVisible ? 'opacity-100 translate-y-0' : 'opacity-0 translate-y-20'}`}>
          <div className="container mx-auto px-4 sm:px-6 lg:px-8 mb-24 relative z-10 text-black">
            <div className="max-w-4xl">
              <h2 className="text-7xl md:text-[7rem] font-black tracking-tighter uppercase leading-[0.85] mb-10 transition-all hover:skew-x-1 cursor-default">
                EXPLORA LOS <br />RINCONES.
              </h2>
              <p className="text-black/60 font-black uppercase tracking-[0.1em] text-2xl max-w-2xl text-left">
                Elegí a tu gusto entre sesiones activas en todo el mundo. El poder de la ubicación en tus manos.
              </p>
            </div>
          </div>

          {/* Scrolling Carousel with 16 Unique Sessions - Portrait Orientation */}
          <div className="flex gap-8 overflow-hidden relative group py-6">
            <div className="flex gap-8 animate-carousel whitespace-nowrap hover:[animation-play-state:paused]">
              {[
                { name: "GRILL_MASTER", owner: "SteakHouse", location: "Puerto Madero, BA", devices: 4, category: "RESTAURANTE" },
                { name: "IRON_PULSE", owner: "FitLife", location: "Brooklyn, NY", devices: 15, category: "GIMNASIO" },
                { name: "HEALTH_HUB", owner: "MedCare", location: "Berlin, DE", devices: 22, category: "HOSPITAL" },
                { name: "VIBE_STATION", owner: "Surround", location: "Ibiza, ES", devices: 18, category: "CLUB" },
                { name: "GOURMET_PASS", owner: "Foodie", location: "Paris, FR", devices: 9, category: "RESTAURANTE" },
                { name: "TITAN_TRAIN", owner: "Olypus", location: "Milan, IT", devices: 12, category: "GIMNASIO" },
                { name: "HORIZON_MED", owner: "LifeSync", location: "Tokyo, JP", devices: 35, category: "HOSPITAL" },
                { name: "COFFEE_NET", owner: "Bean & Co", location: "Bogotá, CO", devices: 6, category: "CAFETERÍA" },
                { name: "URBAN_MALL", owner: "CityRetail", location: "Dubai, AE", devices: 54, category: "MALL" },
                { name: "TECH_CAMPUS", owner: "EduSoft", location: "Palo Alto, CA", devices: 28, category: "UNIVERSIDAD" },
                { name: "METRO_PULSE", owner: "TransportX", location: "London, UK", devices: 110, category: "TRANSPORTE" },
                { name: "CINEMA_WORLD", owner: "GlobalFilm", location: "CDMX, MX", devices: 42, category: "CINE" },
                { name: "AIRPORT_GATE", owner: "SkyHub", location: "Singapore, SG", devices: 85, category: "TRANSPORTE" },
                { name: "HOTEL_LUXE", owner: "GrandStay", location: "Miami, FL", devices: 14, category: "HOTEL" },
                { name: "RETAIL_CORNER", owner: "FastShop", location: "Madrid, ES", devices: 7, category: "RETAIL" },
                { name: "SPORT_ARENA", owner: "FanZone", location: "Barcelona, ES", devices: 63, category: "ESTADIO" },
                // Duplicate
                { name: "GRILL_MASTER", owner: "SteakHouse", location: "Puerto Madero, BA", devices: 4, category: "RESTAURANTE" },
                { name: "IRON_PULSE", owner: "FitLife", location: "Brooklyn, NY", devices: 15, category: "GIMNASIO" },
                { name: "HEALTH_HUB", owner: "MedCare", location: "Berlin, DE", devices: 22, category: "HOSPITAL" },
                { name: "VIBE_STATION", owner: "Surround", location: "Ibiza, ES", devices: 18, category: "CLUB" },
                { name: "GOURMET_PASS", owner: "Foodie", location: "Paris, FR", devices: 9, category: "RESTAURANTE" },
                { name: "TITAN_TRAIN", owner: "Olypus", location: "Milan, IT", devices: 12, category: "GIMNASIO" },
                { name: "HORIZON_MED", owner: "LifeSync", location: "Tokyo, JP", devices: 35, category: "HOSPITAL" },
                { name: "COFFEE_NET", owner: "Bean & Co", location: "Bogotá, CO", devices: 6, category: "CAFETERÍA" },
                { name: "URBAN_MALL", owner: "CityRetail", location: "Dubai, AE", devices: 54, category: "MALL" },
                { name: "TECH_CAMPUS", owner: "EduSoft", location: "Palo Alto, CA", devices: 28, category: "UNIVERSIDAD" },
                { name: "METRO_PULSE", owner: "TransportX", location: "London, UK", devices: 110, category: "TRANSPORTE" },
                { name: "CINEMA_WORLD", owner: "GlobalFilm", location: "CDMX, MX", devices: 42, category: "CINE" },
                { name: "AIRPORT_GATE", owner: "SkyHub", location: "Singapore, SG", devices: 85, category: "TRANSPORTE" },
                { name: "HOTEL_LUXE", owner: "GrandStay", location: "Miami, FL", devices: 14, category: "HOTEL" },
                { name: "RETAIL_CORNER", owner: "FastShop", location: "Madrid, ES", devices: 7, category: "RETAIL" },
                { name: "SPORT_ARENA", owner: "FanZone", location: "Barcelona, ES", devices: 63, category: "ESTADIO" },
              ].map((session, i) => (
                <div key={i} className="inline-block w-[240px] shrink-0">
                  <Card className="bg-black text-white border-white/5 p-8 rounded-[3rem] shadow-2xl transition-all hover:scale-[1.05] duration-500 relative overflow-hidden h-[420px] flex flex-col justify-between group/card text-left">
                    <div className="absolute top-0 right-0 p-4 opacity-5 group-hover/card:opacity-10 transition-opacity">
                       <Monitor size={180} className="-rotate-12 translate-x-12 translate-y-12" />
                    </div>
                    
                    <div className="relative z-10 text-white">
                      <div className="flex justify-between items-start mb-8 text-left">
                        <Badge className="bg-primary/10 text-primary border border-primary/20 font-black text-[8px] px-2 py-0.5 uppercase tracking-widest">{session.category}</Badge>
                        <div className="h-12 w-12 rounded-2xl bg-white/5 flex items-center justify-center text-primary border border-white/10 group-hover/card:bg-primary group-hover/card:text-black transition-colors duration-500">
                          <Monitor size={24} strokeWidth={2.5} />
                        </div>
                      </div>
                      <Badge className="bg-primary text-black font-black text-[10px] px-4 py-1 rounded-full shadow-[0_0_20px_rgba(236,212,68,0.3)] mb-6 w-fit block tracking-widest">LIVE</Badge>
                      <h3 className="font-black text-2xl tracking-tighter mb-2 uppercase truncate">{session.name}</h3>
                      <p className="text-[10px] font-black text-white/30 uppercase tracking-[0.2em] truncate">{session.location}</p>
                    </div>
                    
                    <div className="relative z-10 border-t border-white/5 pt-8 mt-auto text-white">
                      <div className="space-y-4">
                        <div className="flex flex-col gap-1">
                          <span className="text-[9px] font-black text-white/20 uppercase tracking-widest">Partner</span>
                          <span className="text-sm font-black text-white/80 uppercase truncate">{session.owner}</span>
                        </div>
                        <div className="flex flex-col gap-1">
                          <span className="text-[9px] font-black text-white/20 uppercase tracking-widest">Scale</span>
                          <span className="text-lg font-black text-primary italic leading-none">{session.devices} UNITS</span>
                        </div>
                      </div>
                    </div>
                  </Card>
                </div>
              ))}
            </div>
          </div>
        </section>

        {/* Analytics & Optimization Section */}
        <section id="beneficios" ref={analyticsReveal.ref} className={`py-40 bg-background relative overflow-hidden transition-all duration-1000 ${analyticsReveal.isVisible ? 'opacity-100 translate-y-0' : 'opacity-0 translate-y-20'}`}>
          <div className="absolute top-1/2 right-0 -translate-y-1/2 w-full h-full bg-[radial-gradient(circle_at_70%_50%,rgba(236,212,68,0.05),transparent_50%)] pointer-events-none" />
          <div className="container mx-auto px-4 sm:px-6 lg:px-8 relative z-10 text-white text-center lg:text-left">
            <div className="grid grid-cols-1 lg:grid-cols-2 gap-32 items-center">
              <div className="flex flex-col items-center lg:items-start">
                <Badge variant="outline" className="mb-8 px-6 py-1.5 text-[11px] font-black uppercase tracking-[0.4em] border-primary/50 text-primary rounded-full">
                  Real-Time Analytics
                </Badge>
                <h2 className="text-7xl md:text-[7rem] font-black tracking-tighter text-foreground leading-[0.85] mb-16 uppercase italic">
                  DATOS QUE <br />
                  <span className="text-primary italic font-black">IMPULSAN.</span>
                </h2>
                <p className="text-2xl text-foreground/60 font-medium leading-relaxed mb-16 uppercase tracking-tight max-w-xl italic">
                  No adivines más. Tomá decisiones basadas en datos reales. Ajustá tu estrategia sobre la marcha.
                </p>
                
                <div className="space-y-10 w-full">
                  {[
                    { icon: BarChart3, title: "Métricas en Vivo", desc: "Visualizá impresiones, alcance y frecuencia en tiempo real." },
                    { icon: Zap, title: "Control Directo", desc: "Pausá y ajustá tus presupuestos con total autonomía desde el panel." },
                    { icon: ShieldCheck, title: "Historial de Sesiones", desc: "Registro detallado y auditable de cada reproducción en la red." }
                  ].map((feat, i) => (
                    <div key={i} className="flex gap-8 group cursor-default text-left">
                      <div className="flex h-16 w-16 shrink-0 items-center justify-center rounded-[1.5rem] bg-white/5 text-primary border border-white/10 group-hover:bg-primary group-hover:text-black transition-all duration-500 shadow-2xl">
                        <feat.icon size={32} strokeWidth={2.5} />
                      </div>
                      <div>
                        <h3 className="text-xl font-black text-foreground tracking-tight mb-2 uppercase group-hover:text-primary transition-colors">{feat.title}</h3>
                        <p className="text-base text-foreground/40 font-bold uppercase tracking-tighter leading-snug">{feat.desc}</p>
                      </div>
                    </div>
                  ))}
                </div>
              </div>
              
              <div className="relative group">
                <div className="aspect-square rounded-[4rem] bg-gradient-to-br from-white/5 to-transparent p-1 border border-white/10 relative overflow-hidden shadow-2xl group-hover:scale-[1.02] transition-transform duration-700">
                  <div className="absolute inset-0 bg-primary/5 opacity-50" />
                  <div className="relative h-full w-full p-12 md:p-16 flex flex-col justify-between backdrop-blur-sm text-white">
                    <div className="flex justify-between items-start">
                       <div className="space-y-2 text-left">
                          <p className="text-[11px] font-black text-white/30 uppercase tracking-[0.3em]">Campaña Activa</p>
                          <p className="text-3xl font-black text-white uppercase tracking-tighter">Summer_Launch_2026</p>
                       </div>
                       <Badge className="bg-green-500/20 text-green-500 border-none font-black text-[11px] px-4 py-1 rounded-full uppercase tracking-widest">ACTIVE</Badge>
                    </div>
                    
                    <div className="grid grid-cols-2 gap-12 text-left">
                      <div className="space-y-2">
                        <p className="text-[11px] font-black text-white/30 uppercase tracking-[0.3em]">Impresiones</p>
                        <p className="text-6xl font-black text-primary tabular-nums tracking-tighter">84.2K</p>
                      </div>
                      <div className="space-y-2">
                        <p className="text-[11px] font-black text-white/30 uppercase tracking-[0.3em]">Sesiones</p>
                        <p className="text-6xl font-black text-white tabular-nums tracking-tighter">+120</p>
                      </div>
                    </div>
                    
                    <div className="h-56 w-full bg-white/5 rounded-[2.5rem] border border-white/5 flex items-end p-8 gap-4 relative overflow-hidden group/chart">
                       <div className="absolute top-6 left-8 flex items-center gap-3">
                          <div className="h-3 w-3 rounded-full bg-primary animate-pulse" />
                          <span className="text-xs font-black text-primary uppercase tracking-[0.3em]">Performance Live Feed</span>
                       </div>
                       {[60, 40, 85, 50, 100, 75, 45, 90, 65, 55, 80, 60].map((h, i) => (
                         <div 
                           key={i} 
                           className="flex-1 bg-primary/20 rounded-t-xl hover:bg-primary transition-all duration-500 cursor-crosshair relative group/bar" 
                           style={{ height: `${h}%` }}
                         >
                            <div className="absolute -top-10 left-1/2 -translate-x-1/2 bg-primary text-black text-[10px] font-black px-2 py-1 rounded-lg opacity-0 group-hover/bar:opacity-100 transition-opacity whitespace-nowrap uppercase shadow-2xl">
                               +{h}%
                            </div>
                         </div>
                       ))}
                    </div>
                  </div>
                </div>
                <div className="absolute -top-16 -right-16 h-48 w-48 bg-primary/10 rounded-full blur-[100px] animate-pulse" />
                <div className="absolute -bottom-16 -left-16 h-64 w-64 bg-primary/5 rounded-full blur-[120px]" />
              </div>
            </div>
          </div>
        </section>

        {/* CTA Section */}
        <section className="py-48 bg-primary text-black overflow-hidden relative mx-4 rounded-[5rem] mb-12 shadow-[0_40px_100px_rgba(236,212,68,0.2)]">
          <div className="container mx-auto px-4 sm:px-6 lg:px-8 text-center relative z-10">
            <h2 className="text-5xl md:text-7xl lg:text-8xl font-black tracking-tighter leading-none uppercase mb-10 text-black italic">
             Sé parte de la red donde todos ganan.
            </h2>
            <p className="text-black/60 font-black uppercase tracking-tight text-xl md:text-2xl max-w-3xl mx-auto mb-16">
              Registrate y empezá a pujar hoy. <br />
              Encendé tu pantalla. Empezá a facturar en tiempo real.
            </p>
            <div className="flex justify-center">
              <Link to="/login">
                <Button size="lg" className="px-20 h-24 text-3xl font-black bg-background text-foreground hover:bg-background/90 rounded-[2rem] border-none uppercase transition-all hover:scale-110 shadow-2xl group text-white">
                  EMPEZAR AHORA <ArrowRight className="ml-4 h-10 w-10 group-hover:translate-x-2 transition-transform text-white" />
                </Button>
              </Link>
            </div>
          </div>
          <div className="absolute inset-0 opacity-10 pointer-events-none" style={{ backgroundImage: 'radial-gradient(circle at 3px 3px, black 1px, transparent 0)', backgroundSize: '48px 48px' }} />
        </section>
      </main>

      <footer className="border-t border-white/5 py-24 bg-background">
        <div className="container mx-auto px-4 sm:px-6 lg:px-8 text-white">
          <div className="flex flex-col md:flex-row justify-between items-center gap-16 text-center md:text-left">
            <Link to="/" className="flex items-center gap-4 text-foreground group">
              <div className="flex h-12 w-12 items-center justify-center rounded-2xl bg-primary text-black group-hover:rotate-12 transition-transform duration-500">
                <Tv size={28} strokeWidth={2.5} />
              </div>
              <span className="text-3xl font-black tracking-tighter uppercase tracking-widest text-white">BIDCAST</span>
            </Link>
            <p className="text-xs font-black text-foreground/20 uppercase tracking-[0.4em]">
              © {new Date().getFullYear()} BIDCAST TECH. ALL RIGHTS RESERVED.
            </p>
            <div className="flex gap-12">
              {['Twitter', 'GitHub', 'LinkedIn'].map(social => (
                <a key={social} href="#" className="text-xs font-black text-foreground/30 hover:text-primary uppercase tracking-[0.3em] transition-colors">{social}</a>
              ))}
            </div>
          </div>
        </div>
      </footer>
    </div>
  )
}

export default LandingPage
