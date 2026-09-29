import React, { useEffect, useRef } from 'react';

interface Star {
  x: number;
  y: number;
  size: number;
  speed: number;
  brightness: number;
  twinkleSpeed: number;
  twinklePhase: number;
}

export const StarBackground: React.FC = () => {
  const canvasRef = useRef<HTMLCanvasElement | null>(null);

  useEffect(() => {
    const canvas = canvasRef.current;
    if (!canvas) return;

    const ctx = canvas.getContext('2d');
    if (!ctx) return;

    let animationFrameId: number;
    let width = (canvas.width = window.innerWidth);
    let height = (canvas.height = window.innerHeight);

    const handleResize = () => {
      if (!canvas) return;
      width = canvas.width = window.innerWidth;
      height = canvas.height = window.innerHeight;
    };

    window.addEventListener('resize', handleResize);

    // Generate stars
    const starCount = Math.floor((width * height) / 4500);
    const stars: Star[] = Array.from({ length: Math.max(80, starCount) }, () => ({
      x: Math.random() * width,
      y: Math.random() * height,
      size: Math.random() * 1.8 + 0.4,
      speed: Math.random() * 0.15 + 0.05,
      brightness: Math.random() * 0.7 + 0.3,
      twinkleSpeed: Math.random() * 0.02 + 0.005,
      twinklePhase: Math.random() * Math.PI * 2,
    }));

    let time = 0;

    const render = () => {
      time += 1;
      ctx.clearRect(0, 0, width, height);

      // Subtle background nebula gradient
      const gradient = ctx.createRadialGradient(
        width * 0.5,
        height * 0.45,
        100,
        width * 0.5,
        height * 0.5,
        Math.max(width, height) * 0.8
      );
      gradient.addColorStop(0, '#0a1026');
      gradient.addColorStop(0.45, '#060a17');
      gradient.addColorStop(1, '#02040a');

      ctx.fillStyle = gradient;
      ctx.fillRect(0, 0, width, height);

      // Render stars
      for (let i = 0; i < stars.length; i++) {
        const star = stars[i];

        // Star movement - slow upward/drift
        star.y -= star.speed;
        if (star.y < 0) {
          star.y = height;
          star.x = Math.random() * width;
        }

        // Twinkle calculation
        const twinkle = Math.sin(time * star.twinkleSpeed + star.twinklePhase) * 0.3 + 0.7;
        const currentAlpha = Math.min(1, Math.max(0.1, star.brightness * twinkle));

        ctx.beginPath();
        ctx.arc(star.x, star.y, star.size, 0, Math.PI * 2);

        // Slight cyan-white glow for larger stars
        if (star.size > 1.4) {
          ctx.fillStyle = `rgba(186, 230, 253, ${currentAlpha})`;
          ctx.shadowBlur = 4;
          ctx.shadowColor = 'rgba(56, 189, 248, 0.6)';
        } else {
          ctx.fillStyle = `rgba(240, 246, 255, ${currentAlpha})`;
          ctx.shadowBlur = 0;
        }

        ctx.fill();
      }

      ctx.shadowBlur = 0;
      animationFrameId = requestAnimationFrame(render);
    };

    render();

    return () => {
      window.removeEventListener('resize', handleResize);
      cancelAnimationFrame(animationFrameId);
    };
  }, []);

  return (
    <canvas
      ref={canvasRef}
      className="fixed inset-0 pointer-events-none z-0"
      style={{ display: 'block' }}
    />
  );
};
